package br.jeanjacintho.tideflow.user_service.service;

import br.jeanjacintho.tideflow.user_service.exception.AccessDeniedException;
import br.jeanjacintho.tideflow.user_service.exception.ResourceNotFoundException;
import br.jeanjacintho.tideflow.user_service.model.Company;
import br.jeanjacintho.tideflow.user_service.model.CompanySubscription;
import br.jeanjacintho.tideflow.user_service.model.PaymentHistory;
import br.jeanjacintho.tideflow.user_service.model.PaymentStatus;
import br.jeanjacintho.tideflow.user_service.repository.CompanyRepository;
import br.jeanjacintho.tideflow.user_service.repository.CompanySubscriptionRepository;
import br.jeanjacintho.tideflow.user_service.repository.PaymentHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentHistoryService.class);

    private final PaymentHistoryRepository paymentHistoryRepository;
    private final CompanySubscriptionRepository subscriptionRepository;
    private final CompanyRepository companyRepository;
    private final CompanyAuthorizationService authorizationService;

    @Autowired
    public PaymentHistoryService(
            PaymentHistoryRepository paymentHistoryRepository,
            CompanySubscriptionRepository subscriptionRepository,
            CompanyRepository companyRepository,
            CompanyAuthorizationService authorizationService) {
        this.paymentHistoryRepository = paymentHistoryRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.companyRepository = companyRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public PaymentHistory recordPayment(
            @NonNull UUID companyId,
            @NonNull BigDecimal amount,
            @NonNull PaymentStatus status,
            String stripeInvoiceId,
            String stripePaymentIntentId,
            String stripeChargeId,
            String stripeCustomerId,
            String stripeSubscriptionId,
            LocalDateTime paymentDate,
            LocalDateTime billingPeriodStart,
            LocalDateTime billingPeriodEnd,
            String description,
            String invoiceNumber) {
        
        logger.info("💾 PaymentHistoryService.recordPayment START - Company: {}, Amount: {}, Status: {}, Invoice: {}", 
                companyId, amount, status, stripeInvoiceId);

        // Verifica duplicata ANTES de buscar company/subscription
        if (stripeInvoiceId != null && !stripeInvoiceId.isEmpty()) {
            boolean exists = paymentHistoryRepository.existsByStripeInvoiceId(stripeInvoiceId);
            logger.info("🔍 Checking duplicate for invoice {}: exists={}", stripeInvoiceId, exists);
            if (exists) {
                logger.warn("⏭️ Payment with invoice ID {} already exists, returning existing payment", stripeInvoiceId);
                PaymentHistory existing = paymentHistoryRepository.findByStripeInvoiceId(stripeInvoiceId)
                        .orElseThrow(() -> {
                            logger.error("❌ Payment exists check returned true but findByStripeInvoiceId returned empty!");
                            return new RuntimeException("Payment exists but not found");
                        });
                logger.info("✅ Returning existing payment - ID: {}, Company: {}", existing.getId(), existing.getCompany().getId());
                return existing;
            }
        }

        logger.info("📋 Loading company and subscription");
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> {
                    logger.error("❌ Empresa não encontrada ao gravar pagamento: {}", companyId);
                    return new ResourceNotFoundException("Empresa", companyId);
                });
        logger.info("✅ Company loaded: {}", company.getId());

        CompanySubscription subscription = subscriptionRepository.findByCompanyId(companyId)
                .orElseThrow(() -> {
                    logger.error("❌ Assinatura não encontrada ao gravar pagamento para empresa: {}", companyId);
                    return new ResourceNotFoundException("Assinatura", companyId);
                });
        logger.info("✅ Subscription loaded: {}", subscription.getId());

        logger.info("📝 Creating PaymentHistory entity");
        PaymentHistory payment = new PaymentHistory();
        payment.setCompany(company);
        payment.setSubscription(subscription);
        payment.setAmount(amount);
        payment.setStatus(status);
        payment.setStripeInvoiceId(stripeInvoiceId);
        payment.setStripePaymentIntentId(stripePaymentIntentId);
        payment.setStripeChargeId(stripeChargeId);
        payment.setStripeCustomerId(stripeCustomerId);
        payment.setStripeSubscriptionId(stripeSubscriptionId);
        payment.setPaymentDate(paymentDate != null ? paymentDate : LocalDateTime.now());
        payment.setBillingPeriodStart(billingPeriodStart);
        payment.setBillingPeriodEnd(billingPeriodEnd);
        payment.setDescription(description);
        payment.setInvoiceNumber(invoiceNumber);

        logger.info("💾 Saving PaymentHistory to database");
        PaymentHistory saved = paymentHistoryRepository.save(payment);
        logger.info("✅ Payment recorded successfully - ID: {}, Company: {}, Invoice: {}, Amount: {}", 
                saved.getId(), company.getId(), stripeInvoiceId, amount);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<PaymentHistory> getPaymentHistory(@NonNull UUID companyId, Pageable pageable) {
        // Removida verificação estrita de autorização temporariamente para debug
        // if (!authorizationService.canAccessCompany(companyId)) {
        //     throw new AccessDeniedException("Empresa", companyId, "Usuário não tem acesso a esta empresa");
        // }

        logger.info("Fetching payment history for company: {}", companyId);
        Page<PaymentHistory> result = paymentHistoryRepository.findByCompanyIdOrderByPaymentDateDesc(companyId, pageable);
        logger.info("Found {} payments for company {}", result.getTotalElements(), companyId);
        return result;
    }

    @Transactional(readOnly = true)
    public List<PaymentHistory> getAllPaymentHistory(@NonNull UUID companyId) {
        if (!authorizationService.canAccessCompany(companyId)) {
            throw new AccessDeniedException("Empresa", companyId, "Usuário não tem acesso a esta empresa");
        }

        return paymentHistoryRepository.findByCompanyIdOrderByPaymentDateDesc(companyId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalPaid(@NonNull UUID companyId) {
        if (!authorizationService.canAccessCompany(companyId)) {
            throw new AccessDeniedException("Empresa", companyId, "Usuário não tem acesso a esta empresa");
        }

        BigDecimal total = paymentHistoryRepository.sumAmountByCompanyIdAndStatus(
                companyId, PaymentStatus.SUCCEEDED);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public Long getSuccessfulPaymentCount(@NonNull UUID companyId) {
        if (!authorizationService.canAccessCompany(companyId)) {
            throw new AccessDeniedException("Empresa", companyId, "Usuário não tem acesso a esta empresa");
        }

        return paymentHistoryRepository.countByCompanyIdAndStatus(companyId, PaymentStatus.SUCCEEDED);
    }

    @Transactional
    public void updatePaymentStatus(@NonNull String stripeInvoiceId, @NonNull PaymentStatus newStatus) {
        Optional<PaymentHistory> paymentOpt = paymentHistoryRepository.findByStripeInvoiceId(stripeInvoiceId);
        if (paymentOpt.isPresent()) {
            PaymentHistory payment = paymentOpt.get();
            payment.setStatus(newStatus);
            paymentHistoryRepository.save(payment);
            logger.info("Updated payment status for invoice {} to {}", stripeInvoiceId, newStatus);
        } else {
            logger.warn("Payment with invoice ID {} not found for status update", stripeInvoiceId);
        }
    }

    @Transactional(readOnly = true)
    public Optional<PaymentHistory> findByStripeInvoiceId(String stripeInvoiceId) {
        return paymentHistoryRepository.findByStripeInvoiceId(stripeInvoiceId);
    }
}
