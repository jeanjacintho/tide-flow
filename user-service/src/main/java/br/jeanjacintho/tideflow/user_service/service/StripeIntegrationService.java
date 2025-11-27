package br.jeanjacintho.tideflow.user_service.service;

import br.jeanjacintho.tideflow.user_service.dto.request.CreateCheckoutSessionRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.CheckoutSessionResponseDTO;
import br.jeanjacintho.tideflow.user_service.exception.ResourceNotFoundException;
import br.jeanjacintho.tideflow.user_service.model.BillingCycle;
import br.jeanjacintho.tideflow.user_service.model.Company;
import br.jeanjacintho.tideflow.user_service.model.CompanySubscription;
import br.jeanjacintho.tideflow.user_service.model.PaymentHistory;
import br.jeanjacintho.tideflow.user_service.model.PaymentStatus;
import br.jeanjacintho.tideflow.user_service.model.SubscriptionPlan;
import br.jeanjacintho.tideflow.user_service.model.SubscriptionStatus;
import br.jeanjacintho.tideflow.user_service.repository.CompanyRepository;
import br.jeanjacintho.tideflow.user_service.repository.CompanySubscriptionRepository;
import br.jeanjacintho.tideflow.user_service.repository.PaymentHistoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import com.stripe.param.InvoiceListParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class StripeIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(StripeIntegrationService.class);

    private final StripeService stripeService;
    private final CompanySubscriptionRepository subscriptionRepository;
    private final CompanyRepository companyRepository;
    private final SubscriptionService subscriptionService;
    private final UsageTrackingService usageTrackingService;
    private final PaymentHistoryService paymentHistoryService;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final ObjectMapper objectMapper;

    public StripeIntegrationService(StripeService stripeService,
                                    CompanySubscriptionRepository subscriptionRepository,
                                    CompanyRepository companyRepository,
                                    SubscriptionService subscriptionService,
                                    UsageTrackingService usageTrackingService,
                                    PaymentHistoryService paymentHistoryService,
                                    PaymentHistoryRepository paymentHistoryRepository,
                                    ObjectMapper objectMapper) {
        this.stripeService = stripeService;
        this.subscriptionRepository = subscriptionRepository;
        this.companyRepository = companyRepository;
        this.subscriptionService = subscriptionService;
        this.usageTrackingService = usageTrackingService;
        this.paymentHistoryService = paymentHistoryService;
        this.paymentHistoryRepository = paymentHistoryRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Inicia o fluxo de checkout no Stripe garantindo todos os pré-requisitos.
     */
    public CheckoutSessionResponseDTO startCheckoutSession(CreateCheckoutSessionRequestDTO request) {
        CompanySubscription subscription = ensureLocalSubscription(request.companyId());
        Company company = ensureCompanyLoaded(subscription, request.companyId());
        String customerId = ensureStripeCustomer(subscription, company);
        String priceId = resolvePriceId();

        String successUrl = request.successUrl() != null
                ? request.successUrl()
                : stripeService.frontendUrl + "/subscription?success=true";

        String cancelUrl = request.cancelUrl() != null
                ? request.cancelUrl()
                : stripeService.frontendUrl + "/subscription?canceled=true";

        try {
            String checkoutUrl = stripeService.createCheckoutSession(
                    subscription.getCompany().getId(),
                    customerId,
                    priceId,
                    successUrl,
                    cancelUrl
            );
            return new CheckoutSessionResponseDTO(checkoutUrl, null);
        } catch (StripeException e) {
            logger.error("Erro ao criar checkout session no Stripe: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao criar checkout session: " + e.getMessage(), e);
        }
    }

    /**
     * Cancela uma assinatura no Stripe e sincroniza o estado local.
     */
    public void cancelSubscription(UUID companyId) {
        CompanySubscription subscription = subscriptionRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura", companyId));

        String stripeSubscriptionId = subscription.getStripeSubscriptionId();
        if (stripeSubscriptionId == null) {
            logger.info("Assinatura da empresa {} não possui subscription do Stripe vinculada.", companyId);
        } else {
            try {
                stripeService.cancelSubscription(stripeSubscriptionId);
                logger.info("Stripe subscription {} cancelada com sucesso.", stripeSubscriptionId);
            } catch (StripeException e) {
                logger.error("Erro ao cancelar assinatura no Stripe: {}", e.getMessage(), e);
                throw new RuntimeException("Não foi possível cancelar a assinatura no Stripe: " + e.getMessage(), e);
            }
        }

        // Atualiza estado local para FREE
        String stripeCustomerId = subscription.getStripeCustomerId() != null
                ? subscription.getStripeCustomerId()
                : (stripeSubscriptionId != null ? fetchCustomerFromStripe(stripeSubscriptionId) : null);

        if (stripeCustomerId != null) {
            subscriptionService.cancelStripeSubscription(stripeCustomerId);
        } else {
            subscription.setStatus(SubscriptionStatus.CANCELLED);
            subscription.setPlanType(SubscriptionPlan.FREE);
            subscription.setPricePerUser(BigDecimal.ZERO);
            subscriptionRepository.save(subscription);
            Company company = subscription.getCompany();
            company.setSubscriptionPlan(SubscriptionPlan.FREE);
            company.setMaxEmployees(7);
            companyRepository.save(company);
        }
    }

    /**
     * Processa todos os webhooks vindos do Stripe.
     */
    public void processWebhook(String payload, String signatureHeader) {
        logger.info("📥 Webhook received - Payload length: {}, Signature present: {}", 
                payload != null ? payload.length() : 0, signatureHeader != null && !signatureHeader.isEmpty());
        
        String webhookSecret = stripeService.getWebhookSecret();
        if (webhookSecret == null || webhookSecret.isEmpty()) {
            logger.error("❌ Webhook secret not configured");
            throw new IllegalStateException("Webhook secret not configured");
        }

        if (signatureHeader == null || signatureHeader.isEmpty()) {
            logger.error("❌ Missing Stripe-Signature header");
            throw new IllegalArgumentException("Missing Stripe-Signature header");
        }

        try {
            Event event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
            logger.info("✅ Webhook event verified - Type: {}, ID: {}", event.getType(), event.getId());

            switch (event.getType()) {
                case "checkout.session.completed":
                    logger.info("🎯 Routing to handleCheckoutSessionCompleted");
                    handleCheckoutSessionCompleted(event);
                    break;
                case "customer.subscription.created":
                case "customer.subscription.updated":
                    logger.info("🎯 Routing to handleSubscriptionUpdated");
                    handleSubscriptionUpdated(event);
                    break;
                case "customer.subscription.deleted":
                    logger.info("🎯 Routing to handleSubscriptionDeleted");
                    handleSubscriptionDeleted(event);
                    break;
                case "invoice.payment_succeeded":
                case "invoice.paid":
                    logger.info("🎯 Routing to handleInvoicePaymentSucceeded");
                    handleInvoicePaymentSucceeded(event);
                    break;
                case "invoice.finalized":
                    logger.info("🎯 Routing to handleInvoiceFinalized");
                    handleInvoiceFinalized(event);
                    break;
                case "invoice.payment_failed":
                    logger.info("🎯 Routing to handleInvoicePaymentFailed");
                    handleInvoicePaymentFailed(event);
                    break;
                default:
                    logger.debug("⏭️ Unhandled event type: {}", event.getType());
            }
            logger.info("✅ Webhook processing completed successfully - Type: {}, ID: {}", event.getType(), event.getId());
        } catch (SignatureVerificationException e) {
            logger.error("❌ Webhook signature verification failed: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid Stripe signature");
        } catch (Exception e) {
            logger.error("❌ Erro ao processar webhook: {}", e.getMessage(), e);
            logger.error("Stack trace:", e);
            throw new RuntimeException("Erro ao processar webhook: " + e.getMessage(), e);
        }
    }

    /**
     * Força sincronização de assinatura e histórico diretamente do Stripe.
     */
    public Map<String, Object> forceSyncWithStripe(UUID companyId) {
        Map<String, Object> result = new HashMap<>();
        CompanySubscription subscription = subscriptionRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura", companyId));

        if (subscription.getStripeSubscriptionId() == null) {
            throw new IllegalStateException("Assinatura não possui Stripe Subscription ID");
        }

        String stripeCustomerId = subscription.getStripeCustomerId();
        if (stripeCustomerId == null || stripeCustomerId.isEmpty()) {
            throw new IllegalStateException("Assinatura não possui Stripe Customer ID");
        }
        
        // Após a verificação, sabemos que stripeCustomerId não é null
        final String nonNullCustomerId = stripeCustomerId;

        try {
            Subscription stripeSubscription = stripeService.getSubscription(subscription.getStripeSubscriptionId());
            subscriptionService.syncSubscriptionFromStripe(nonNullCustomerId, stripeSubscription);
            syncRecentPaymentsFromStripe(subscription.getStripeSubscriptionId(), companyId);
            result.put("success", true);
            result.put("stripeSubscriptionId", subscription.getStripeSubscriptionId());
        } catch (StripeException e) {
            logger.error("Erro ao sincronizar assinatura: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao sincronizar com Stripe: " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * Endpoint utilitário para testar as estratégias de lookup.
     */
    public Map<String, Object> testSubscriptionLookup(String subscriptionId, String customerId, String invoiceId) {
        Map<String, Object> result = new HashMap<>();
        result.put("subscriptionId", subscriptionId);
        result.put("customerId", customerId);
        result.put("invoiceId", invoiceId);

        CompanySubscription subscription = findSubscriptionRobustly(subscriptionId, customerId, invoiceId);
        if (subscription != null) {
            result.put("found", true);
            result.put("companyId", subscription.getCompany().getId().toString());
            result.put("subscriptionDbId", subscription.getId().toString());
            result.put("planType", subscription.getPlanType().name());
            result.put("stripeCustomerId", subscription.getStripeCustomerId());
            result.put("stripeSubscriptionId", subscription.getStripeSubscriptionId());
        } else {
            result.put("found", false);
            result.put("error", "Subscription not found by any strategy");
        }
        return result;
    }

    /**
     * Verifica metadados diretamente no Stripe.
     */
    public Map<String, Object> checkStripeMetadata(String subscriptionId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Subscription stripeSub = stripeService.getSubscription(subscriptionId);
            Map<String, String> metadata = stripeSub.getMetadata();
            result.put("subscriptionId", subscriptionId);
            result.put("metadata", metadata != null ? metadata : new HashMap<>());
            result.put("hasCompanyId", metadata != null && metadata.containsKey("company_id"));
            result.put("companyId", metadata != null ? metadata.get("company_id") : null);
            result.put("stripeCustomerId", stripeSub.getCustomer());
            result.put("status", stripeSub.getStatus());
            return result;
        } catch (Exception e) {
            logger.error("Error checking Stripe metadata: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao consultar metadados no Stripe: " + e.getMessage(), e);
        }
    }

    /* -------------------------------------------------------
       Métodos privados utilitários
     ------------------------------------------------------- */

    private CompanySubscription ensureLocalSubscription(UUID companyId) {
        return subscriptionRepository.findByCompanyId(companyId)
                .orElseGet(() -> createFreeSubscription(companyId));
    }

    private CompanySubscription createFreeSubscription(UUID companyId) {
        logger.info("Criando assinatura FREE para companyId {}", companyId);
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID cannot be null");
        }
        // Após a verificação, sabemos que companyId não é null
        final UUID nonNullCompanyId = companyId;
        Company company = companyRepository.findById(nonNullCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", nonNullCompanyId));

        int currentUserCount = usageTrackingService.getActiveUserCount(nonNullCompanyId);
        CompanySubscription subscription = new CompanySubscription(
                company,
                SubscriptionPlan.FREE,
                BigDecimal.ZERO,
                currentUserCount,
                BillingCycle.MONTHLY,
                LocalDateTime.now().plusMonths(1)
        );
        subscription.setStatus(SubscriptionStatus.TRIAL);
        subscription = subscriptionRepository.save(subscription);

        company.setSubscriptionPlan(SubscriptionPlan.FREE);
        company.setMaxEmployees(7);
        companyRepository.save(company);

        return subscription;
    }

    private Company ensureCompanyLoaded(CompanySubscription subscription, UUID companyId) {
        Company company = subscription.getCompany();
        if (company == null) {
            if (companyId == null) {
                throw new IllegalArgumentException("Company ID cannot be null");
            }
            final UUID nonNullCompanyId = companyId;
            company = companyRepository.findById(nonNullCompanyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Empresa", nonNullCompanyId));
            subscription.setCompany(company);
        }
        return company;
    }

    private String ensureStripeCustomer(CompanySubscription subscription, Company company) {
        if (subscription.getStripeCustomerId() != null && !subscription.getStripeCustomerId().isEmpty()) {
            return subscription.getStripeCustomerId();
        }

        String email;
        if (company.getBillingEmail() != null && !company.getBillingEmail().trim().isEmpty()) {
            email = company.getBillingEmail();
        } else if (company.getDomain() != null && !company.getDomain().trim().isEmpty()) {
            email = "billing@" + company.getDomain().trim();
        } else {
            email = "billing-" + company.getId().toString().substring(0, 8) + "@tideflow.local";
        }

        String customerName = company.getName() != null && !company.getName().trim().isEmpty()
                ? company.getName()
                : "Empresa " + company.getId().toString().substring(0, 8);

        try {
            String customerId = stripeService.getOrCreateCustomer(company.getId(), customerName, email);
            subscription.setStripeCustomerId(customerId);
            subscriptionRepository.save(subscription);
            return customerId;
        } catch (StripeException e) {
            logger.error("Erro ao criar/obter customer no Stripe: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao criar customer no Stripe: " + e.getMessage(), e);
        }
    }

    private String resolvePriceId() {
        try {
            return stripeService.getOrCreateEnterprisePrice();
        } catch (StripeException e) {
            throw new RuntimeException("Erro ao resolver price no Stripe: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("null")
    private void handleCheckoutSessionCompleted(Event event) {
        logger.info("Processing checkout.session.completed - Event {}", event.getId());
        try {
            JsonNode dataObject = objectMapper.readTree(event.toJson()).path("data").path("object");
            if (dataObject.isMissingNode()) {
                logger.error("checkout.session.completed sem data.object");
                return;
            }

            String subscriptionId = dataObject.path("subscription").asText(null);
            String customerId = dataObject.path("customer").asText(null);
            String companyIdStr = dataObject.path("metadata").path("company_id").asText(null);

            if (subscriptionId == null || customerId == null || companyIdStr == null) {
                logger.error("Dados insuficientes no checkout.session.completed");
                return;
            }

            UUID companyId = UUID.fromString(companyIdStr);
            // Após UUID.fromString, sabemos que companyId não é null
            final UUID nonNullCompanyId = companyId;
            try {
                Subscription stripeSubscription = stripeService.getSubscription(subscriptionId);
                subscriptionService.activateStripeSubscription(nonNullCompanyId, customerId, stripeSubscription);
                recordInitialPayment(nonNullCompanyId, customerId, subscriptionId, stripeSubscription);
            } catch (StripeException e) {
                logger.error("Erro Stripe ao ativar assinatura: {}", e.getMessage(), e);
                subscriptionService.activateStripeSubscription(nonNullCompanyId, customerId, subscriptionId);
            }
        } catch (Exception e) {
            logger.error("Erro no handleCheckoutSessionCompleted: {}", e.getMessage(), e);
        }
    }

    private void handleSubscriptionUpdated(Event event) {
        logger.info("🔄 Processing customer.subscription.updated - Event {}", event.getId());
        try {
            JsonNode dataObject = objectMapper.readTree(event.toJson()).path("data").path("object");
            if (dataObject.isMissingNode()) {
                logger.warn("⚠️ subscription.updated event missing data.object");
                return;
            }
            String subscriptionId = dataObject.path("id").asText(null);
            String customerId = dataObject.path("customer").asText(null);
            if (subscriptionId == null || customerId == null) {
                logger.warn("⚠️ subscription.updated missing subscriptionId or customerId");
                return;
            }
            
            logger.info("📋 Syncing subscription - subscriptionId: {}, customerId: {}", subscriptionId, customerId);
            Subscription stripeSubscription = stripeService.getSubscription(subscriptionId);
            subscriptionService.syncSubscriptionFromStripe(customerId, stripeSubscription);
            
            // Verifica invoices recentes pagas que podem não ter sido registradas
            checkAndRecordRecentPaidInvoices(subscriptionId, customerId);
        } catch (Exception e) {
            logger.error("❌ Erro ao processar subscription.updated: {}", e.getMessage(), e);
        }
    }

    private void handleSubscriptionDeleted(Event event) {
        logger.info("Processing customer.subscription.deleted - Event {}", event.getId());
        try {
            JsonNode dataObject = objectMapper.readTree(event.toJson()).path("data").path("object");
            String customerId = dataObject.path("customer").asText(null);
            if (customerId != null) {
                subscriptionService.cancelStripeSubscription(customerId);
            }
        } catch (Exception e) {
            logger.error("Erro ao processar subscription.deleted: {}", e.getMessage(), e);
        }
    }

    private void handleInvoicePaymentSucceeded(Event event) {
        logger.info("🔄 Processing invoice payment success - Event {}", event.getId());
        try {
            JsonNode invoice = objectMapper.readTree(event.toJson()).path("data").path("object");
            String invoiceId = invoice.path("id").asText(null);
            String subscriptionId = invoice.path("subscription").asText(null);
            String customerId = invoice.path("customer").asText(null);
            String invoiceStatus = invoice.path("status").asText(null);

            logger.info("📄 Invoice details - ID: {}, Subscription: {}, Customer: {}, Status: {}", 
                    invoiceId, subscriptionId, customerId, invoiceStatus);

            // Extração robusta do valor pago
            BigDecimal amountPaid = null;
            if (invoice.has("amount_paid") && !invoice.path("amount_paid").isNull()) {
                long amountPaidCents = invoice.path("amount_paid").asLong(0);
                amountPaid = BigDecimal.valueOf(amountPaidCents).divide(BigDecimal.valueOf(100));
                logger.info("💰 Using amount_paid: {} (cents: {})", amountPaid, amountPaidCents);
            } else if (invoice.has("total") && !invoice.path("total").isNull()) {
                long totalCents = invoice.path("total").asLong(0);
                amountPaid = BigDecimal.valueOf(totalCents).divide(BigDecimal.valueOf(100));
                logger.info("💰 Using total: {} (cents: {})", amountPaid, totalCents);
            } else if (invoice.has("amount_due") && !invoice.path("amount_due").isNull()) {
                long amountDueCents = invoice.path("amount_due").asLong(0);
                amountPaid = BigDecimal.valueOf(amountDueCents).divide(BigDecimal.valueOf(100));
                logger.info("💰 Using amount_due: {} (cents: {})", amountPaid, amountDueCents);
            } else {
                amountPaid = BigDecimal.ZERO;
                logger.warn("⚠️ No amount field found, defaulting to ZERO");
            }

            if (invoiceId == null) {
                logger.error("❌ Invoice ID is null, cannot process payment");
                return;
            }

            // Se subscriptionId não estiver no evento, busca diretamente do Stripe
            Invoice invoiceFromStripe = null;
            if (subscriptionId == null || subscriptionId.isEmpty()) {
                logger.info("🔍 Subscription ID not in event, fetching invoice from Stripe: {}", invoiceId);
                try {
                    invoiceFromStripe = Invoice.retrieve(invoiceId);
                    subscriptionId = invoiceFromStripe.getSubscription();
                    if (subscriptionId != null && !subscriptionId.isEmpty()) {
                        logger.info("✅ Found subscriptionId from Stripe invoice: {}", subscriptionId);
                    } else {
                        logger.warn("⚠️ Invoice {} has no subscription, may be a one-time payment", invoiceId);
                        // Para pagamentos únicos sem subscription, ainda podemos registrar usando apenas customerId
                        if (customerId != null && !customerId.isEmpty()) {
                            CompanySubscription subscription = findSubscriptionRobustly(null, customerId, invoiceId);
                            if (subscription != null) {
                                logger.info("✅ Found subscription by customerId for one-time payment");
                                subscriptionId = subscription.getStripeSubscriptionId(); // Pode ser null, mas tentamos
                            }
                        }
                    }
                    
                    // Se amountPaid ainda for null ou zero, tenta usar o valor do invoice do Stripe
                    if ((amountPaid == null || amountPaid.compareTo(BigDecimal.ZERO) == 0) && invoiceFromStripe.getAmountPaid() != null && invoiceFromStripe.getAmountPaid() > 0) {
                        amountPaid = BigDecimal.valueOf(invoiceFromStripe.getAmountPaid()).divide(BigDecimal.valueOf(100));
                        logger.info("💰 Updated amountPaid from Stripe invoice: {}", amountPaid);
                    }
                } catch (Exception e) {
                    logger.error("❌ Error fetching invoice from Stripe: {}", e.getMessage(), e);
                }
            }

            if (subscriptionId == null || subscriptionId.isEmpty()) {
                logger.error("❌ Subscription ID is still null for invoice {} after Stripe lookup, cannot process payment", invoiceId);
                // Mesmo sem subscriptionId, tentamos registrar usando apenas customerId se disponível
                if (customerId == null || customerId.isEmpty()) {
                    logger.error("❌ Customer ID also null, cannot process payment");
                    return;
                }
                logger.warn("⚠️ Attempting to process payment without subscriptionId, using customerId only");
            }

            logger.info("🔍 Looking up subscription - subscriptionId: {}, customerId: {}, invoiceId: {}", 
                    subscriptionId, customerId, invoiceId);

            // Busca subscription mesmo se subscriptionId for null (usa customerId ou invoiceId)
            CompanySubscription subscription = findSubscriptionRobustly(
                    subscriptionId != null && !subscriptionId.isEmpty() ? subscriptionId : null, 
                    customerId, 
                    invoiceId);
            if (subscription == null) {
                logger.error("❌ Não foi possível localizar assinatura para invoice {} - subscriptionId: {}, customerId: {}", 
                        invoiceId, subscriptionId, customerId);
                return;
            }
            
            // Se encontramos a subscription mas subscriptionId estava null, atualiza
            if ((subscriptionId == null || subscriptionId.isEmpty()) && subscription.getStripeSubscriptionId() != null) {
                subscriptionId = subscription.getStripeSubscriptionId();
                logger.info("🔧 Updated subscriptionId from found subscription: {}", subscriptionId);
            }

            logger.info("✅ Subscription found - Company: {}, Subscription DB ID: {}", 
                    subscription.getCompany().getId(), subscription.getId());

            Long periodStart = invoice.has("period_start") && !invoice.path("period_start").isNull() 
                    ? invoice.path("period_start").asLong() : null;
            Long periodEnd = invoice.has("period_end") && !invoice.path("period_end").isNull() 
                    ? invoice.path("period_end").asLong() : null;
            String paymentIntentId = invoice.path("payment_intent").asText(null);
            String chargeId = invoice.path("charge").asText(null);
            String description = invoice.path("description").asText(null);
            String invoiceNumber = invoice.path("number").asText(null);

            logger.info("💾 Recording payment - Invoice: {}, Amount: {}, Company: {}", 
                    invoiceId, amountPaid, subscription.getCompany().getId());

            recordPaymentSafely(
                    subscription.getCompany().getId(),
                    amountPaid,
                    PaymentStatus.SUCCEEDED,
                    invoiceId,
                    paymentIntentId,
                    chargeId,
                    customerId,
                    subscriptionId,
                    periodStart,
                    periodEnd,
                    description,
                    invoiceNumber
            );

            logger.info("🔄 Syncing subscription state");
            syncSubscriptionState(subscription, customerId, subscriptionId);
            logger.info("✅ Successfully processed invoice payment for invoice {}", invoiceId);
        } catch (Exception e) {
            logger.error("❌ Erro ao processar invoice payment succeeded: {}", e.getMessage(), e);
            logger.error("Stack trace:", e);
        }
    }

    private void handleInvoiceFinalized(Event event) {
        logger.info("🔄 Processing invoice.finalized - Event {}", event.getId());
        try {
            JsonNode invoice = objectMapper.readTree(event.toJson()).path("data").path("object");
            String invoiceId = invoice.path("id").asText(null);
            String subscriptionId = invoice.path("subscription").asText(null);
            String customerId = invoice.path("customer").asText(null);
            String invoiceStatus = invoice.path("status").asText(null);

            logger.info("📄 Invoice finalized - ID: {}, Status: {}, Subscription: {}, Customer: {}", invoiceId, invoiceStatus, subscriptionId, customerId);

            // Se a invoice já está paga quando é finalizada, registra imediatamente
            if ("paid".equals(invoiceStatus)) {
                logger.info("💰 Invoice {} is already paid when finalized, recording payment", invoiceId);
                handleInvoicePaymentSucceeded(event);
            } else {
                logger.info("⏭️ Invoice {} finalized but not yet paid (status: {}), will be recorded when payment succeeds", 
                        invoiceId, invoiceStatus);
            }
        } catch (Exception e) {
            logger.error("❌ Erro ao processar invoice.finalized: {}", e.getMessage(), e);
        }
    }

    private void handleInvoicePaymentFailed(Event event) {
        logger.info("Processing invoice.payment_failed - Event {}", event.getId());
        try {
            JsonNode invoice = objectMapper.readTree(event.toJson()).path("data").path("object");
            String invoiceId = invoice.path("id").asText(null);
            String subscriptionId = invoice.path("subscription").asText(null);
            String customerId = invoice.path("customer").asText(null);

            if (invoiceId == null || invoiceId.isEmpty()) {
                logger.error("Invoice ID is null or empty, cannot process payment failure");
                return;
            }

            CompanySubscription subscription = findSubscriptionRobustly(subscriptionId, customerId, invoiceId);
            if (subscription == null) {
                logger.error("Não foi possível localizar assinatura para invoice falha {}", invoiceId);
                return;
            }

            BigDecimal amountDue = invoice.has("amount_due")
                    ? BigDecimal.valueOf(invoice.path("amount_due").asLong()).divide(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            // Após verificação, sabemos que invoiceId não é null
            final String nonNullInvoiceId = invoiceId;
            PaymentHistoryRepository repository = this.paymentHistoryRepository;
            repository.findByStripeInvoiceId(nonNullInvoiceId).ifPresentOrElse(
                    history -> paymentHistoryService.updatePaymentStatus(nonNullInvoiceId, PaymentStatus.FAILED),
                    () -> recordPaymentSafely(
                            subscription.getCompany().getId(),
                            amountDue,
                            PaymentStatus.FAILED,
                            nonNullInvoiceId,
                            invoice.path("payment_intent").asText(null),
                            invoice.path("charge").asText(null),
                            customerId,
                            subscriptionId,
                            invoice.has("period_start") ? invoice.path("period_start").asLong() : null,
                            invoice.has("period_end") ? invoice.path("period_end").asLong() : null,
                            invoice.path("description").asText(null),
                            invoice.path("number").asText(null)
                    )
            );

            syncSubscriptionState(subscription, customerId, subscriptionId);
        } catch (Exception e) {
            logger.error("Erro ao processar invoice.payment_failed: {}", e.getMessage(), e);
        }
    }

    private CompanySubscription findSubscriptionRobustly(String subscriptionId, String customerId, String invoiceId) {
        logger.info("🔍 Subscription lookup START - subscriptionId={}, customerId={}, invoiceId={}", 
                subscriptionId, customerId, invoiceId);

        // Estratégia 1: Buscar por customerId
        if (customerId != null && !customerId.isEmpty()) {
            logger.info("🔍 Strategy 1: Looking up by customerId: {}", customerId);
            CompanySubscription byCustomer = subscriptionRepository.findByStripeCustomerId(customerId).orElse(null);
            if (byCustomer != null) {
                logger.info("✅ Found subscription by customerId - Company: {}, Subscription DB ID: {}", 
                        byCustomer.getCompany().getId(), byCustomer.getId());
                return byCustomer;
            }
            logger.info("❌ No subscription found by customerId");
        }

        // Estratégia 2: Buscar por subscriptionId
        if (subscriptionId != null && !subscriptionId.isEmpty()) {
            logger.info("🔍 Strategy 2: Looking up by subscriptionId: {}", subscriptionId);
            CompanySubscription bySubscription = subscriptionRepository.findByStripeSubscriptionId(subscriptionId).orElse(null);
            if (bySubscription != null) {
                logger.info("✅ Found subscription by subscriptionId - Company: {}, Subscription DB ID: {}", 
                        bySubscription.getCompany().getId(), bySubscription.getId());
                return bySubscription;
            }
            logger.info("❌ No subscription found by subscriptionId");
        }

        // Estratégia 3: Buscar no Stripe e usar metadata
        if (subscriptionId != null && !subscriptionId.isEmpty()) {
            logger.info("🔍 Strategy 3: Fetching subscription from Stripe and checking metadata");
            try {
                Subscription stripeSub = stripeService.getSubscription(subscriptionId);
                Map<String, String> metadata = stripeSub.getMetadata();
                logger.info("📋 Stripe subscription metadata: {}", metadata);
                
                if (metadata != null && metadata.containsKey("company_id")) {
                    String companyIdStr = metadata.get("company_id");
                    logger.info("📋 Found company_id in metadata: {}", companyIdStr);
                    try {
                        UUID companyId = UUID.fromString(companyIdStr);
                        CompanySubscription byCompany = subscriptionRepository.findByCompanyId(companyId).orElse(null);
                        if (byCompany != null) {
                            logger.info("✅ Found subscription by companyId from metadata - Company: {}, Subscription DB ID: {}", 
                                    byCompany.getCompany().getId(), byCompany.getId());
                            
                            // Atualiza IDs se necessário
                            boolean updated = false;
                            if (byCompany.getStripeSubscriptionId() == null || !byCompany.getStripeSubscriptionId().equals(subscriptionId)) {
                                logger.info("🔧 Updating stripeSubscriptionId: {} -> {}", 
                                        byCompany.getStripeSubscriptionId(), subscriptionId);
                                byCompany.setStripeSubscriptionId(subscriptionId);
                                updated = true;
                            }
                            if (byCompany.getStripeCustomerId() == null || !byCompany.getStripeCustomerId().equals(stripeSub.getCustomer())) {
                                logger.info("🔧 Updating stripeCustomerId: {} -> {}", 
                                        byCompany.getStripeCustomerId(), stripeSub.getCustomer());
                                byCompany.setStripeCustomerId(stripeSub.getCustomer());
                                updated = true;
                            }
                            if (updated) {
                                subscriptionRepository.save(byCompany);
                                logger.info("✅ Updated subscription with Stripe IDs");
                            }
                            return byCompany;
                        } else {
                            logger.warn("⚠️ Company {} found in metadata but no subscription exists", companyId);
                        }
                    } catch (IllegalArgumentException e) {
                        logger.error("❌ Invalid company_id format in metadata: {}", companyIdStr, e);
                    }
                } else {
                    logger.warn("⚠️ No company_id found in subscription metadata");
                }

                // Estratégia 3b: Usar customerId do Stripe
                String stripeCustomerId = stripeSub.getCustomer();
                if (stripeCustomerId != null && !stripeCustomerId.isEmpty()) {
                    logger.info("🔍 Strategy 3b: Looking up by customerId from Stripe: {}", stripeCustomerId);
                    CompanySubscription byCustomer = subscriptionRepository.findByStripeCustomerId(stripeCustomerId).orElse(null);
                    if (byCustomer != null) {
                        logger.info("✅ Found subscription by customerId from Stripe - Company: {}, Subscription DB ID: {}", 
                                byCustomer.getCompany().getId(), byCustomer.getId());
                        return byCustomer;
                    }
                }
            } catch (Exception e) {
                logger.error("❌ Erro ao consultar subscription no Stripe: {}", e.getMessage(), e);
            }
        }

        // Estratégia 4: Buscar por invoiceId (se já existe histórico)
        if (invoiceId != null && !invoiceId.isEmpty()) {
            logger.info("🔍 Strategy 4: Looking up by invoiceId: {}", invoiceId);
            return paymentHistoryRepository.findByStripeInvoiceId(invoiceId)
                    .map(history -> {
                        logger.info("✅ Found payment history for invoice, using company: {}", history.getCompany().getId());
                        return subscriptionRepository.findByCompanyId(history.getCompany().getId()).orElse(null);
                    })
                    .orElse(null);
        }

        logger.error("❌ All lookup strategies failed - subscriptionId: {}, customerId: {}, invoiceId: {}", 
                subscriptionId, customerId, invoiceId);
        return null;
    }

    private void recordPaymentSafely(UUID companyId,
                                     BigDecimal amount,
                                     PaymentStatus status,
                                     String invoiceId,
                                     String paymentIntentId,
                                     String chargeId,
                                     String customerId,
                                     String subscriptionId,
                                     Long periodStart,
                                     Long periodEnd,
                                     String description,
                                     String invoiceNumber) {

        logger.info("💰 Recording payment - Invoice: {}, Amount: {}, Status: {}, Company: {}", 
                invoiceId, amount, status, companyId);
        
        // Verifica se já existe
        if (paymentHistoryRepository.findByStripeInvoiceId(invoiceId).isPresent()) {
            logger.info("⏭️ Payment for invoice {} already exists. Skipping duplicate.", invoiceId);
            return;
        }

        // Validações de null para parâmetros obrigatórios
        if (companyId == null) {
            logger.error("❌ Company ID is null, cannot record payment for invoice {}", invoiceId);
            throw new IllegalArgumentException("Company ID cannot be null");
        }
        if (amount == null) {
            logger.error("❌ Amount is null, cannot record payment for invoice {}", invoiceId);
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (status == null) {
            logger.error("❌ Payment status is null, cannot record payment for invoice {}", invoiceId);
            throw new IllegalArgumentException("Payment status cannot be null");
        }

        // Após validações, sabemos que são não-null
        final UUID nonNullCompanyId = companyId;
        final BigDecimal nonNullAmount = amount;
        final PaymentStatus nonNullStatus = status;

        logger.info("💾 Calling paymentHistoryService.recordPayment for invoice {}", invoiceId);
        try {
            PaymentHistory saved = paymentHistoryService.recordPayment(
                    nonNullCompanyId,
                    nonNullAmount,
                    nonNullStatus,
                    invoiceId,
                    paymentIntentId,
                    chargeId,
                    customerId,
                    subscriptionId,
                    null,
                    periodStart != null ? java.time.LocalDateTime.ofEpochSecond(periodStart, 0, java.time.ZoneOffset.UTC) : null,
                    periodEnd != null ? java.time.LocalDateTime.ofEpochSecond(periodEnd, 0, java.time.ZoneOffset.UTC) : null,
                    description,
                    invoiceNumber
            );
            logger.info("✅ Payment recorded successfully - ID: {}, Invoice: {}, Amount: {}", 
                    saved.getId(), invoiceId, amount);
        } catch (Exception e) {
            logger.error("❌ Error recording payment for invoice {}: {}", invoiceId, e.getMessage(), e);
            throw e; // Re-throw para que o erro seja visível nos logs
        }
    }

    private void recordInitialPayment(UUID companyId,
                                      String customerId,
                                      String subscriptionId,
                                      Subscription stripeSubscription) {
        logger.info("🔄 Recording initial payment - Company: {}, Subscription: {}", companyId, subscriptionId);
        try {
            String latestInvoiceId = stripeSubscription.getLatestInvoice();
            if (latestInvoiceId == null) {
                logger.warn("⚠️ No latest invoice found for subscription {}", subscriptionId);
                return;
            }

            logger.info("📄 Retrieving invoice: {}", latestInvoiceId);
            Invoice invoice = Invoice.retrieve(latestInvoiceId);
            String invoiceStatus = invoice.getStatus();
            Long amountPaidCents = invoice.getAmountPaid();
            
            logger.info("📄 Invoice status: {}, amountPaid: {}", invoiceStatus, amountPaidCents);
            
            if (!"paid".equals(invoiceStatus)) {
                logger.warn("⚠️ Invoice {} is not paid (status: {}), skipping", latestInvoiceId, invoiceStatus);
                return;
            }

            if (amountPaidCents == null) {
                logger.warn("⚠️ Invoice {} has null amountPaid, skipping", latestInvoiceId);
                return;
            }

            BigDecimal amountPaid = BigDecimal.valueOf(amountPaidCents).divide(BigDecimal.valueOf(100));
            logger.info("💰 Recording initial payment - Invoice: {}, Amount: {}", latestInvoiceId, amountPaid);
            
            recordPaymentSafely(
                    companyId,
                    amountPaid,
                    PaymentStatus.SUCCEEDED,
                    invoice.getId(),
                    invoice.getPaymentIntent(),
                    invoice.getCharge(),
                    customerId,
                    subscriptionId,
                    invoice.getPeriodStart(),
                    invoice.getPeriodEnd(),
                    invoice.getDescription(),
                    invoice.getNumber()
            );
            logger.info("✅ Initial payment recorded successfully");
        } catch (Exception e) {
            logger.error("❌ Não foi possível registrar pagamento inicial: {}", e.getMessage(), e);
        }
    }

    private void syncSubscriptionState(CompanySubscription subscription, String customerId, String subscriptionId) {
        try {
            Subscription stripeSubscription = stripeService.getSubscription(subscriptionId);
            String resolvedCustomerId = customerId != null ? customerId : stripeSubscription.getCustomer();
            if (resolvedCustomerId == null || resolvedCustomerId.isEmpty()) {
                logger.error("❌ Customer ID is null or empty, cannot sync subscription state for subscription {}", subscriptionId);
                return;
            }
            // Após verificação, sabemos que resolvedCustomerId não é null
            final String nonNullCustomerId = resolvedCustomerId;
            subscriptionService.syncSubscriptionFromStripe(nonNullCustomerId, stripeSubscription);
        } catch (Exception e) {
            logger.error("Erro ao sincronizar assinatura: {}", e.getMessage(), e);
        }
    }

    /**
     * Verifica invoices recentes pagas que podem não ter sido registradas.
     * Busca as últimas 10 invoices da subscription e registra quaisquer pagas que ainda não estejam no histórico.
     */
    private void checkAndRecordRecentPaidInvoices(String subscriptionId, String customerId) {
        logger.info("🔍 Checking recent paid invoices for subscription: {}", subscriptionId);
        try {
            List<Invoice> invoices = Invoice.list(
                    InvoiceListParams.builder()
                            .setSubscription(subscriptionId)
                            .setLimit(10L)
                            .build()
            ).getData();

            logger.info("📋 Found {} recent invoices for subscription {}", invoices.size(), subscriptionId);

            CompanySubscription subscription = findSubscriptionRobustly(subscriptionId, customerId, null);
            if (subscription == null) {
                logger.warn("⚠️ Could not find subscription to record payments for subscriptionId: {}", subscriptionId);
                return;
            }

            int recordedCount = 0;
            for (Invoice invoice : invoices) {
                String invoiceId = invoice.getId();
                String invoiceStatus = invoice.getStatus();
                Long amountPaidCents = invoice.getAmountPaid();

                logger.info("📄 Checking invoice {} - Status: {}, AmountPaid: {}", invoiceId, invoiceStatus, amountPaidCents);

                // Verifica se já existe no histórico
                if (paymentHistoryRepository.findByStripeInvoiceId(invoiceId).isPresent()) {
                    logger.info("⏭️ Invoice {} already exists in payment history", invoiceId);
                    continue;
                }

                // Registra apenas se estiver paga e tiver valor > 0
                if ("paid".equals(invoiceStatus) && amountPaidCents != null && amountPaidCents > 0) {
                    logger.info("💰 Found paid invoice not in history - Invoice: {}, Amount: {}", invoiceId, amountPaidCents);
                    
                    BigDecimal amountPaid = BigDecimal.valueOf(amountPaidCents).divide(BigDecimal.valueOf(100));
                    recordPaymentSafely(
                            subscription.getCompany().getId(),
                            amountPaid,
                            PaymentStatus.SUCCEEDED,
                            invoice.getId(),
                            invoice.getPaymentIntent(),
                            invoice.getCharge(),
                            customerId,
                            subscriptionId,
                            invoice.getPeriodStart(),
                            invoice.getPeriodEnd(),
                            invoice.getDescription(),
                            invoice.getNumber()
                    );
                    recordedCount++;
                } else {
                    logger.info("⏭️ Invoice {} not eligible - Status: {}, Amount: {}", invoiceId, invoiceStatus, amountPaidCents);
                }
            }

            if (recordedCount > 0) {
                logger.info("✅ Recorded {} new payment(s) from recent invoices check", recordedCount);
            } else {
                logger.info("✅ No new payments found in recent invoices");
            }
        } catch (Exception e) {
            logger.error("❌ Error checking recent paid invoices for subscription {}: {}", subscriptionId, e.getMessage(), e);
        }
    }

    private void syncRecentPaymentsFromStripe(String stripeSubscriptionId, UUID companyId) {
        try {
            List<Invoice> invoices = Invoice.list(
                    InvoiceListParams.builder()
                            .setSubscription(stripeSubscriptionId)
                            .setLimit(10L)
                            .build()
            ).getData();

            for (Invoice invoice : invoices) {
                if (!"paid".equals(invoice.getStatus()) || invoice.getAmountPaid() == null || invoice.getAmountPaid() <= 0) {
                    continue;
                }

                if (paymentHistoryRepository.findByStripeInvoiceId(invoice.getId()).isPresent()) {
                    continue;
                }

                BigDecimal amount = BigDecimal.valueOf(invoice.getAmountPaid()).divide(BigDecimal.valueOf(100));
                recordPaymentSafely(
                        companyId,
                        amount,
                        PaymentStatus.SUCCEEDED,
                        invoice.getId(),
                        invoice.getPaymentIntent(),
                        invoice.getCharge(),
                        invoice.getCustomer(),
                        stripeSubscriptionId,
                        invoice.getPeriodStart(),
                        invoice.getPeriodEnd(),
                        invoice.getDescription(),
                        invoice.getNumber()
                );
            }
        } catch (Exception e) {
            logger.error("Erro ao sincronizar pagamentos recentes: {}", e.getMessage());
        }
    }

    private String fetchCustomerFromStripe(String stripeSubscriptionId) {
        try {
            Subscription subscription = stripeService.getSubscription(stripeSubscriptionId);
            return subscription.getCustomer();
        } catch (StripeException e) {
            logger.warn("Não foi possível recuperar customerId a partir da subscription {}", stripeSubscriptionId);
            return null;
        }
    }
}

