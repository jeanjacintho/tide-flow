package br.jeanjacintho.tideflow.user_service.controller;

import br.jeanjacintho.tideflow.user_service.dto.request.UpgradeSubscriptionRequestDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.InvoiceResponseDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.SubscriptionResponseDTO;
import br.jeanjacintho.tideflow.user_service.dto.response.UsageInfoResponseDTO;
import br.jeanjacintho.tideflow.user_service.model.CompanySubscription;
import br.jeanjacintho.tideflow.user_service.model.SubscriptionPlan;
import br.jeanjacintho.tideflow.user_service.service.BillingService;
import br.jeanjacintho.tideflow.user_service.service.SubscriptionService;
import br.jeanjacintho.tideflow.user_service.service.UsageTrackingService;
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

    public SubscriptionController(
            SubscriptionService subscriptionService,
            BillingService billingService,
            UsageTrackingService usageTrackingService) {
        this.subscriptionService = subscriptionService;
        this.billingService = billingService;
        this.usageTrackingService = usageTrackingService;
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
}
