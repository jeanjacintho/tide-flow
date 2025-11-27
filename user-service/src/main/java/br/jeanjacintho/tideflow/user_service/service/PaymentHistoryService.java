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
        
        logger.info("Recording payment for company {}: amount={}, status={}, invoiceId={}", 
                companyId, amount, status, stripeInvoiceId);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", companyId));

        CompanySubscription subscription = subscriptionRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura", companyId));

        if (stripeInvoiceId != null && paymentHistoryRepository.existsByStripeInvoiceId(stripeInvoiceId)) {
            logger.warn("Payment with invoice ID {} already exists, skipping duplicate", stripeInvoiceId);
            return paymentHistoryRepository.findByStripeInvoiceId(stripeInvoiceId)
                    .orElseThrow(() -> new RuntimeException("Payment exists but not found"));
        }

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

        PaymentHistory saved = paymentHistoryRepository.save(payment);
        logger.info("Payment recorded successfully with ID: {}", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<PaymentHistory> getPaymentHistory(@NonNull UUID companyId, Pageable pageable) {
        if (!authorizationService.canAccessCompany(companyId)) {
            throw new AccessDeniedException("Empresa", companyId, "Usuário não tem acesso a esta empresa");
        }

        return paymentHistoryRepository.findByCompanyIdOrderByPaymentDateDesc(companyId, pageable);
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
