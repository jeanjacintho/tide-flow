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
import br.jeanjacintho.tideflow.user_service.model.BillingCycle;
import br.jeanjacintho.tideflow.user_service.model.SubscriptionStatus;
import br.jeanjacintho.tideflow.user_service.repository.CompanyRepository;
import br.jeanjacintho.tideflow.user_service.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import br.jeanjacintho.tideflow.user_service.service.BillingService;
import br.jeanjacintho.tideflow.user_service.service.PaymentHistoryService;
import br.jeanjacintho.tideflow.user_service.service.SubscriptionService;
import br.jeanjacintho.tideflow.user_service.service.StripeIntegrationService;
import br.jeanjacintho.tideflow.user_service.service.UsageTrackingService;
import br.jeanjacintho.tideflow.user_service.repository.CompanySubscriptionRepository;
import br.jeanjacintho.tideflow.user_service.dto.response.PaymentHistoryResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import br.jeanjacintho.tideflow.user_service.model.PaymentHistory;
import br.jeanjacintho.tideflow.user_service.repository.PaymentHistoryRepository;

@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin(origins = "*")
public class SubscriptionController {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionController.class);

    private final SubscriptionService subscriptionService;
    private final BillingService billingService;
    private final UsageTrackingService usageTrackingService;
    private final StripeIntegrationService stripeIntegrationService;
    private final PaymentHistoryService paymentHistoryService;
    private final CompanySubscriptionRepository subscriptionRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final CompanyRepository companyRepository;

    public SubscriptionController(
            SubscriptionService subscriptionService,
            BillingService billingService,
            UsageTrackingService usageTrackingService,
            StripeIntegrationService stripeIntegrationService,
            PaymentHistoryService paymentHistoryService,
            CompanySubscriptionRepository subscriptionRepository,
            PaymentHistoryRepository paymentHistoryRepository,
            CompanyRepository companyRepository) {
        this.subscriptionService = subscriptionService;
        this.billingService = billingService;
        this.usageTrackingService = usageTrackingService;
        this.stripeIntegrationService = stripeIntegrationService;
        this.paymentHistoryService = paymentHistoryService;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentHistoryRepository = paymentHistoryRepository;
        this.companyRepository = companyRepository;
    }

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

    @GetMapping("/companies/{companyId}")
    public ResponseEntity<SubscriptionResponseDTO> getSubscription(@PathVariable @NonNull UUID companyId) {
        logger.info("GET /api/subscriptions/companies/{}", companyId);

        try {

            CompanySubscription subscription = subscriptionRepository.findByCompanyId(companyId)
                    .orElse(null);

            if (subscription == null) {
                logger.info("Assinatura não encontrada para companyId: {}. Criando assinatura FREE...", companyId);

                Company company = companyRepository.findById(companyId)
                        .orElseThrow(() -> new ResourceNotFoundException("Empresa", companyId));

                int currentUserCount = usageTrackingService.getActiveUserCount(companyId);
                subscription = new CompanySubscription(
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

                logger.info("Assinatura FREE criada automaticamente para companyId: {}", companyId);
            }

            BigDecimal monthlyBill = subscriptionService.calculateMonthlyBill(companyId);
            SubscriptionResponseDTO response = SubscriptionResponseDTO.fromEntity(subscription, monthlyBill);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            logger.error("Empresa não encontrada: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Erro ao obter assinatura: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

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

    @PostMapping("/checkout-session")
    public ResponseEntity<CheckoutSessionResponseDTO> createCheckoutSession(
            @Valid @RequestBody CreateCheckoutSessionRequestDTO request) {
        logger.info("POST /api/subscriptions/checkout-session - companyId: {}", request.companyId());
        try {
            CheckoutSessionResponseDTO responseDTO = stripeIntegrationService.startCheckoutSession(request);
            return ResponseEntity.ok(responseDTO);
        } catch (ResourceNotFoundException e) {
            logger.error("Recurso não encontrado ao criar checkout session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            logger.error("Argumento inválido ao criar checkout session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (RuntimeException e) {
            logger.error("Erro ao criar checkout session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CheckoutSessionResponseDTO(null, null));
        }
    }

    @DeleteMapping("/companies/{companyId}")
    public ResponseEntity<Void> cancelCompanySubscription(@PathVariable @NonNull UUID companyId) {
        logger.info("DELETE /api/subscriptions/companies/{} - cancel subscription", companyId);
        try {
            stripeIntegrationService.cancelSubscription(companyId);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            logger.error("Assinatura não encontrada para cancelamento: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            logger.error("Erro ao cancelar assinatura: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/companies/{companyId}/payments")
    public ResponseEntity<Page<PaymentHistoryResponseDTO>> getPaymentHistory(
            @PathVariable @NonNull UUID companyId,
            @PageableDefault(size = 20, sort = "paymentDate", direction = Sort.Direction.DESC) Pageable pageable) {
        logger.info("GET /api/subscriptions/companies/{}/payments", companyId);

        try {
            Page<PaymentHistoryResponseDTO> payments = paymentHistoryService
                    .getPaymentHistory(companyId, pageable)
                    .map(PaymentHistoryResponseDTO::fromEntity);
            logger.info("Returning {} payments for company {}", payments.getContent().size(), companyId);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            logger.error("Erro ao obter histórico de pagamentos: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/companies/{companyId}/payments/debug")
    public ResponseEntity<Map<String, Object>> debugPaymentHistory(@PathVariable @NonNull UUID companyId) {
        logger.info("GET /api/subscriptions/companies/{}/payments/debug", companyId);

        Map<String, Object> debugInfo = new HashMap<>();
        try {

            CompanySubscription subscription = subscriptionRepository.findByCompanyId(companyId).orElse(null);
            if (subscription != null) {
                debugInfo.put("subscription", Map.of(
                    "id", subscription.getId().toString(),
                    "stripeCustomerId", subscription.getStripeCustomerId() != null ? subscription.getStripeCustomerId() : "null",
                    "stripeSubscriptionId", subscription.getStripeSubscriptionId() != null ? subscription.getStripeSubscriptionId() : "null",
                    "planType", subscription.getPlanType().name(),
                    "status", subscription.getStatus().name()
                ));
            } else {
                debugInfo.put("subscription", "NOT FOUND");
            }

            Long paymentCount = paymentHistoryRepository.count();
            debugInfo.put("totalPaymentsInDatabase", paymentCount);

            List<PaymentHistory> companyPayments = paymentHistoryRepository.findByCompanyIdOrderByPaymentDateDesc(companyId);
            debugInfo.put("companyPayments", companyPayments.stream()
                .map(p -> Map.of(
                    "id", p.getId().toString(),
                    "amount", p.getAmount().toString(),
                    "status", p.getStatus().name(),
                    "invoiceId", p.getStripeInvoiceId() != null ? p.getStripeInvoiceId() : "null",
                    "paymentDate", p.getPaymentDate().toString()
                ))
                .collect(java.util.stream.Collectors.toList()));

            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            logger.error("Erro no debug: {}", e.getMessage(), e);
            debugInfo.put("error", e.getMessage());
            return ResponseEntity.ok(debugInfo);
        }
    }

    @PostMapping("/companies/{companyId}/sync-stripe")
    public ResponseEntity<Map<String, Object>> forceStripeSync(@PathVariable @NonNull UUID companyId) {
        logger.info("POST /api/subscriptions/companies/{}/sync-stripe - Force Stripe sync", companyId);

        try {
            Map<String, Object> result = stripeIntegrationService.forceSyncWithStripe(companyId);
            return ResponseEntity.ok(result);
        } catch (ResourceNotFoundException e) {
            logger.error("Assinatura não encontrada ao forçar sync: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error forcing Stripe sync for company {}: {}", companyId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/test-subscription-lookup")
    public ResponseEntity<Map<String, Object>> testSubscriptionLookup(
            @RequestParam String subscriptionId,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String invoiceId) {

        logger.info("POST /api/subscriptions/test-subscription-lookup - Testing lookup strategies");
        Map<String, Object> result = stripeIntegrationService.testSubscriptionLookup(subscriptionId, customerId, invoiceId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stripe-metadata-check")
    public ResponseEntity<Map<String, Object>> checkStripeMetadata(@RequestParam String subscriptionId) {
        logger.info("GET /api/subscriptions/stripe-metadata-check - Checking metadata for: {}", subscriptionId);
        try {
            Map<String, Object> result = stripeIntegrationService.checkStripeMetadata(subscriptionId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error checking Stripe metadata: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        logger.info("POST /api/subscriptions/webhook received");
        try {
            stripeIntegrationService.processWebhook(payload, sigHeader);
            return ResponseEntity.ok("Success");
        } catch (IllegalArgumentException e) {
            logger.error("Erro ao validar webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Erro ao processar webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing webhook: " + e.getMessage());
        }
    }

}
