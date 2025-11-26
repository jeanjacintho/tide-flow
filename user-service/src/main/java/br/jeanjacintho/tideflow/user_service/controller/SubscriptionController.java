package br.jeanjacintho.tideflow.user_service.controller;

import br.jeanjacintho.tideflow.user_service.dto.request.CreateCheckoutSessionRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.request.UpgradeSubscriptionRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.CheckoutSessionResponseDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.InvoiceResponseDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.SubscriptionResponseDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.UsageInfoResponseDTO;
import br.jeanjacintho.tideflow.user_service.model.Company;
import br.jeanjacintho.tideflow.user_service.model.CompanySubscription;
import br.jeanjacintho.tideflow.user_service.model.SubscriptionPlan;
import br.jeanjacintho.tideflow.user_service.service.BillingService;
import br.jeanjacintho.tideflow.user_service.service.SubscriptionService;
import br.jeanjacintho.tideflow.user_service.service.StripeService;
import br.jeanjacintho.tideflow.user_service.service.UsageTrackingService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin(origins = "*")
public class SubscriptionController {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionController.class);

    private final SubscriptionService subscriptionService;
    private final BillingService billingService;
    private final UsageTrackingService usageTrackingService;
    private final StripeService stripeService;

    public SubscriptionController(
            SubscriptionService subscriptionService,
            BillingService billingService,
            UsageTrackingService usageTrackingService,
            StripeService stripeService) {
        this.subscriptionService = subscriptionService;
        this.billingService = billingService;
        this.usageTrackingService = usageTrackingService;
        this.stripeService = stripeService;
    }

    /**
     * POST /api/subscriptions/companies/{companyId}
     * Cria uma assinatura para uma empresa.
     */
    @PostMapping("/companies/{companyId}")
    public ResponseEntity<SubscriptionResponseDTO> createSubscription(
            @PathVariable @NonNull UUID companyId,
            @RequestParam SubscriptionPlan planType) {
        logger.info("POST /api/subscriptions/companies/{} - planType: {}", companyId, planType);
        
        try {
            CompanySubscription subscription = subscriptionService.createSubscription(companyId, planType);
            BigDecimal monthlyBill = subscriptionService.calculateMonthlyBill(companyId);
            SubscriptionResponseDTO response = SubscriptionResponseDTO.fromEntity(subscription, monthlyBill);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Erro ao criar assinatura: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/subscriptions/companies/{companyId}
     * Obtém a assinatura de uma empresa.
     */
    @GetMapping("/companies/{companyId}")
    public ResponseEntity<SubscriptionResponseDTO> getSubscription(@PathVariable @NonNull UUID companyId) {
        logger.info("GET /api/subscriptions/companies/{}", companyId);
        
        try {
            CompanySubscription subscription = subscriptionService.getSubscription(companyId);
            BigDecimal monthlyBill = subscriptionService.calculateMonthlyBill(companyId);
            SubscriptionResponseDTO response = SubscriptionResponseDTO.fromEntity(subscription, monthlyBill);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao obter assinatura: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * PUT /api/subscriptions/companies/{companyId}/upgrade
     * Faz upgrade da assinatura de uma empresa.
     */
    @PutMapping("/companies/{companyId}/upgrade")
    public ResponseEntity<SubscriptionResponseDTO> upgradeSubscription(
            @PathVariable @NonNull UUID companyId,
            @Valid @RequestBody UpgradeSubscriptionRequestDTO request) {
        logger.info("PUT /api/subscriptions/companies/{}/upgrade - planType: {}", companyId, request.planType());
        
        try {
            CompanySubscription subscription = subscriptionService.upgradeSubscription(companyId, request.planType());
            BigDecimal monthlyBill = subscriptionService.calculateMonthlyBill(companyId);
            SubscriptionResponseDTO response = SubscriptionResponseDTO.fromEntity(subscription, monthlyBill);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Erro ao fazer upgrade: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Erro ao fazer upgrade: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/subscriptions/companies/{companyId}/bill
     * Calcula a fatura mensal de uma empresa.
     */
    @GetMapping("/companies/{companyId}/bill")
    public ResponseEntity<BigDecimal> getMonthlyBill(@PathVariable @NonNull UUID companyId) {
        logger.info("GET /api/subscriptions/companies/{}/bill", companyId);
        
        try {
            BigDecimal bill = subscriptionService.calculateMonthlyBill(companyId);
            return ResponseEntity.ok(bill);
        } catch (Exception e) {
            logger.error("Erro ao calcular fatura: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/subscriptions/companies/{companyId}/invoice
     * Gera uma fatura para um período específico.
     */
    @GetMapping("/companies/{companyId}/invoice")
    public ResponseEntity<InvoiceResponseDTO> generateInvoice(
            @PathVariable @NonNull UUID companyId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") String period) {
        logger.info("GET /api/subscriptions/companies/{}/invoice - period: {}", companyId, period);
        
        try {
            BillingService.InvoiceDTO invoice = billingService.generateInvoice(companyId, period);
            InvoiceResponseDTO response = InvoiceResponseDTO.fromInvoiceDTO(invoice);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao gerar fatura: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /api/subscriptions/companies/{companyId}/payment
     * Processa um pagamento.
     */
    @PostMapping("/companies/{companyId}/payment")
    public ResponseEntity<Void> processPayment(
            @PathVariable @NonNull UUID companyId,
            @RequestParam BigDecimal amount) {
        logger.info("POST /api/subscriptions/companies/{}/payment - amount: {}", companyId, amount);
        
        try {
            boolean success = billingService.processPayment(companyId, amount);
            if (success) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            logger.error("Erro ao processar pagamento: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/subscriptions/companies/{companyId}/usage
     * Obtém informações de uso da empresa.
     */
    @GetMapping("/companies/{companyId}/usage")
    public ResponseEntity<UsageInfoResponseDTO> getUsageInfo(@PathVariable @NonNull UUID companyId) {
        logger.info("GET /api/subscriptions/companies/{}/usage", companyId);
        
        try {
            UsageTrackingService.UsageInfo usageInfo = usageTrackingService.getUsageInfo(companyId);
            UsageInfoResponseDTO response = UsageInfoResponseDTO.fromUsageInfo(usageInfo);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao obter informações de uso: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/subscriptions/companies/{companyId}/can-add-users
     * Verifica se a empresa pode adicionar mais usuários.
     */
    @GetMapping("/companies/{companyId}/can-add-users")
    public ResponseEntity<Boolean> canAddUsers(@PathVariable @NonNull UUID companyId) {
        logger.info("GET /api/subscriptions/companies/{}/can-add-users", companyId);
        
        try {
            boolean canAdd = subscriptionService.canAddUsers(companyId);
            return ResponseEntity.ok(canAdd);
        } catch (Exception e) {
            logger.error("Erro ao verificar se pode adicionar usuários: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /api/subscriptions/checkout-session
     * Cria uma sessão de checkout do Stripe para upgrade de assinatura.
     */
    @PostMapping("/checkout-session")
    public ResponseEntity<CheckoutSessionResponseDTO> createCheckoutSession(
            @Valid @RequestBody CreateCheckoutSessionRequestDTO request) {
        logger.info("POST /api/subscriptions/checkout-session - companyId: {}", request.companyId());
        
        try {
            CompanySubscription subscription = subscriptionService.getSubscription(request.companyId());
            Company company = subscription.getCompany();
            
            // Obtém ou cria customer no Stripe
            String customerId = subscription.getStripeCustomerId();
            if (customerId == null || customerId.isEmpty()) {
                customerId = stripeService.getOrCreateCustomer(
                    request.companyId(),
                    company.getName(),
                    company.getBillingEmail() != null ? company.getBillingEmail() : "billing@" + company.getDomain()
                );
                subscription.setStripeCustomerId(customerId);
                subscriptionService.updateSubscription(subscription);
            }
            
            // Obtém ou cria price no Stripe
            String priceId = stripeService.getOrCreateEnterprisePrice();
            
            // URLs de sucesso e cancelamento
            String successUrl = request.successUrl() != null 
                ? request.successUrl() 
                : stripeService.frontendUrl + "/subscription?success=true";
            String cancelUrl = request.cancelUrl() != null 
                ? request.cancelUrl() 
                : stripeService.frontendUrl + "/subscription?canceled=true";
            
            // Cria sessão de checkout
            String checkoutUrl = stripeService.createCheckoutSession(
                request.companyId(),
                customerId,
                priceId,
                successUrl,
                cancelUrl
            );
            
            CheckoutSessionResponseDTO response = new CheckoutSessionResponseDTO(checkoutUrl, null);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            logger.error("Erro ao criar checkout session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("Erro ao criar checkout session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/subscriptions/webhook
     * Webhook do Stripe para processar eventos de assinatura.
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        logger.info("POST /api/subscriptions/webhook received");
        
        try {
            String webhookSecret = stripeService.getWebhookSecret();
            if (webhookSecret == null || webhookSecret.isEmpty()) {
                logger.warn("Webhook secret not configured - webhook processing disabled");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook secret not configured");
            }
            
            if (sigHeader == null || sigHeader.isEmpty()) {
                logger.warn("Stripe-Signature header is missing");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing Stripe-Signature header");
            }
            
            Event event;
            try {
                event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            } catch (com.stripe.exception.SignatureVerificationException e) {
                logger.error("Webhook signature verification failed: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
            }
            
            logger.info("Webhook event verified - Type: {}, ID: {}", event.getType(), event.getId());
            
            // Processa eventos relevantes
            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted(event);
                    break;
                case "customer.subscription.created":
                    handleSubscriptionUpdated(event);
                    break;
                case "customer.subscription.updated":
                    handleSubscriptionUpdated(event);
                    break;
                case "customer.subscription.deleted":
                    handleSubscriptionDeleted(event);
                    break;
                case "invoice.payment_succeeded":
                    handleInvoicePaymentSucceeded(event);
                    break;
                case "invoice.payment_failed":
                    handleInvoicePaymentFailed(event);
                    break;
                default:
                    logger.debug("Unhandled event type: {}", event.getType());
            }
            
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            logger.error("Erro ao processar webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error processing webhook: " + e.getMessage());
        }
    }

    private void handleCheckoutSessionCompleted(Event event) {
        logger.info("Starting processing of checkout.session.completed - Event ID: {}", event.getId());
        try {
            // Acessa os dados do evento através do JSON string e faz parse manualmente
            String eventJson = event.toJson();
            Gson gson = new Gson();
            JsonObject eventObject = gson.fromJson(eventJson, JsonObject.class);
            JsonObject dataObject = eventObject.getAsJsonObject("data").getAsJsonObject("object");
            
            if (dataObject == null) {
                logger.error("Event data object is null. Event ID: {}", event.getId());
                return;
            }
            
            // Extrai os dados necessários diretamente do JSON
            String sessionId = dataObject.has("id") ? dataObject.get("id").getAsString() : null;
            String customerId = dataObject.has("customer") && !dataObject.get("customer").isJsonNull() 
                    ? dataObject.get("customer").getAsString() : null;
            String subscriptionId = dataObject.has("subscription") && !dataObject.get("subscription").isJsonNull()
                    ? dataObject.get("subscription").getAsString() : null;
            
            logger.info("Extracted from event - Session: {}, Customer: {}, Subscription: {}", 
                    sessionId, customerId, subscriptionId);
            
            // Extrai metadata
            JsonObject metadata = null;
            if (dataObject.has("metadata") && !dataObject.get("metadata").isJsonNull()) {
                metadata = dataObject.getAsJsonObject("metadata");
            }
            
            if (metadata == null || !metadata.has("company_id")) {
                logger.error("Company ID not found in session metadata. Session ID: {}", sessionId);
                logger.error("Event data: {}", dataObject.toString());
                return;
            }
            
            String companyIdStr = metadata.get("company_id").getAsString();
            
            if (companyIdStr == null || companyIdStr.isEmpty()) {
                logger.error("Company ID is empty. Session ID: {}", sessionId);
                return;
            }
            
            UUID companyId;
            try {
                companyId = UUID.fromString(companyIdStr);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid UUID format for company_id: {}. Error: {}", companyIdStr, e.getMessage());
                return;
            }
            
            logger.info("Activation data - Company: {}, Customer: {}, Subscription: {}", 
                    companyId, customerId, subscriptionId);
            
            if (subscriptionId == null || subscriptionId.isEmpty()) {
                logger.error("Subscription ID is null or empty");
                return;
            }
            
            if (customerId == null || customerId.isEmpty()) {
                logger.error("Customer ID is null or empty");
                return;
            }
            
            try {
                // Busca a subscription completa do Stripe
                Subscription stripeSubscription = stripeService.getSubscription(subscriptionId);
                logger.info("Retrieved Stripe subscription: {} - Status: {}", 
                        stripeSubscription.getId(), stripeSubscription.getStatus());
                
                subscriptionService.activateStripeSubscription(companyId, customerId, stripeSubscription);
                logger.info("✅ Successfully activated subscription for company: {}", companyId);
            } catch (StripeException e) {
                logger.error("Stripe API error: {}", e.getMessage(), e);
                // Fallback com IDs
                logger.info("Trying fallback activation with IDs only");
                subscriptionService.activateStripeSubscription(companyId, customerId, subscriptionId);
            } catch (Exception e) {
                logger.error("Failed to activate subscription: {}", e.getMessage(), e);
                throw e;
            }
        } catch (Exception e) {
            logger.error("❌ Unexpected error in handleCheckoutSessionCompleted: {}", e.getMessage(), e);
        }
    }

    private void handleSubscriptionUpdated(Event event) {
        logger.info("Processing subscription updated event - Event ID: {}", event.getId());
        try {
            // Acessa os dados através do JSON string
            String eventJson = event.toJson();
            Gson gson = new Gson();
            JsonObject eventObject = gson.fromJson(eventJson, JsonObject.class);
            JsonObject dataObject = eventObject.getAsJsonObject("data").getAsJsonObject("object");
            
            if (dataObject == null) {
                logger.error("Event data object is null");
                return;
            }
            
            String subscriptionId = dataObject.has("id") ? dataObject.get("id").getAsString() : null;
            String customerId = dataObject.has("customer") && !dataObject.get("customer").isJsonNull()
                    ? dataObject.get("customer").getAsString() : null;
            String status = dataObject.has("status") ? dataObject.get("status").getAsString() : null;
            
            logger.info("Subscription data - ID: {}, Customer: {}, Status: {}", subscriptionId, customerId, status);
            
            if (customerId == null || customerId.isEmpty()) {
                logger.warn("Customer ID is null or empty, cannot sync subscription");
                return;
            }
            
            if (subscriptionId == null || subscriptionId.isEmpty()) {
                logger.warn("Subscription ID is null or empty");
                return;
            }
            
            // Busca a subscription completa do Stripe para ter todos os dados
            Subscription stripeSubscription = stripeService.getSubscription(subscriptionId);
            subscriptionService.syncSubscriptionFromStripe(customerId, stripeSubscription);
            logger.info("✅ Subscription synced successfully for customer: {}", customerId);
            
        } catch (Exception e) {
            logger.error("❌ Error processing subscription updated: {}", e.getMessage(), e);
        }
    }

    private void handleSubscriptionDeleted(Event event) {
        logger.info("Processing subscription deleted event - Event ID: {}", event.getId());
        try {
            // Acessa os dados através do JSON string
            String eventJson = event.toJson();
            Gson gson = new Gson();
            JsonObject eventObject = gson.fromJson(eventJson, JsonObject.class);
            JsonObject dataObject = eventObject.getAsJsonObject("data").getAsJsonObject("object");
            
            if (dataObject == null) {
                logger.error("Event data object is null");
                return;
            }
            
            String subscriptionId = dataObject.has("id") ? dataObject.get("id").getAsString() : null;
            String customerId = dataObject.has("customer") && !dataObject.get("customer").isJsonNull()
                    ? dataObject.get("customer").getAsString() : null;
            
            logger.info("Subscription deleted - ID: {}, Customer: {}", subscriptionId, customerId);
            
            if (customerId == null || customerId.isEmpty()) {
                logger.warn("Customer ID is null or empty, cannot cancel subscription");
                return;
            }
            
            subscriptionService.cancelStripeSubscription(customerId);
            logger.info("✅ Subscription cancelled successfully for customer: {}", customerId);
            
        } catch (Exception e) {
            logger.error("❌ Error processing subscription deleted: {}", e.getMessage(), e);
        }
    }

    private void handleInvoicePaymentSucceeded(Event event) {
        logger.info("Processing invoice.payment_succeeded event - Event ID: {}", event.getId());
        try {
            // Acessa os dados através do JSON string
            String eventJson = event.toJson();
            Gson gson = new Gson();
            JsonObject eventObject = gson.fromJson(eventJson, JsonObject.class);
            JsonObject invoice = eventObject.getAsJsonObject("data").getAsJsonObject("object");
            
            if (invoice == null) {
                logger.error("Invoice object is null");
                return;
            }
            
            String subscriptionId = invoice.has("subscription") && !invoice.get("subscription").isJsonNull()
                    ? invoice.get("subscription").getAsString() : null;
            String customerId = invoice.has("customer") && !invoice.get("customer").isJsonNull()
                    ? invoice.get("customer").getAsString() : null;
            
            logger.info("Invoice payment succeeded - Subscription: {}, Customer: {}", subscriptionId, customerId);
            
            if (subscriptionId != null && customerId != null) {
                 // Sincroniza o estado atual da assinatura
                 Subscription stripeSubscription = stripeService.getSubscription(subscriptionId);
                 subscriptionService.syncSubscriptionFromStripe(customerId, stripeSubscription);
                 logger.info("✅ Subscription synced via invoice payment");
            } else {
                logger.warn("Missing subscription or customer ID in invoice");
            }
        } catch (Exception e) {
            logger.error("❌ Error processing invoice payment: {}", e.getMessage(), e);
        }
    }

    private void handleInvoicePaymentFailed(Event event) {
        logger.info("Processing invoice.payment_failed event");
        // Atualiza status da subscription para PAST_DUE ou CANCELED
    }
}
