package br.jeanjacintho.tideflow.user_service.service;

import br.jeanjacintho.tideflow.user_service.exception.AccessDeniedException;
import br.jeanjacintho.tideflow.user_service.exception.ResourceNotFoundException;
import br.jeanjacintho.tideflow.user_service.model.Company;
import br.jeanjacintho.tideflow.user_service.model.CompanySubscription;
import br.jeanjacintho.tideflow.user_service.model.SubscriptionStatus;
import br.jeanjacintho.tideflow.user_service.repository.CompanyRepository;
import br.jeanjacintho.tideflow.user_service.repository.CompanySubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Service
public class BillingService {

    private static final Logger logger = LoggerFactory.getLogger(BillingService.class);

    private final CompanySubscriptionRepository subscriptionRepository;
    private final CompanyRepository companyRepository;
    private final CompanyAuthorizationService authorizationService;
    private final SubscriptionService subscriptionService;
    private final UsageTrackingService usageTrackingService;

    @Autowired
    public BillingService(
            CompanySubscriptionRepository subscriptionRepository,
            CompanyRepository companyRepository,
            CompanyAuthorizationService authorizationService,
            SubscriptionService subscriptionService,
            UsageTrackingService usageTrackingService) {
        this.subscriptionRepository = subscriptionRepository;
        this.companyRepository = companyRepository;
        this.authorizationService = authorizationService;
        this.subscriptionService = subscriptionService;
        this.usageTrackingService = usageTrackingService;
    }

    @Transactional(readOnly = true)
    public InvoiceDTO generateInvoice(@NonNull UUID companyId, String period) {
        logger.info("Gerando fatura para empresa {} no período {}", companyId, period);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", companyId));

        if (!authorizationService.canAccessCompany(companyId)) {
            throw new AccessDeniedException("Empresa", companyId, "Usuário não tem acesso a esta empresa");
        }

        CompanySubscription subscription = subscriptionRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura", companyId));

        YearMonth yearMonth = YearMonth.parse(period);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        int activeUsers = usageTrackingService.getActiveUserCount(companyId);
        BigDecimal totalAmount = subscriptionService.calculateMonthlyBill(companyId);

        return new InvoiceDTO(
            companyId,
            company.getName(),
            period,
            startDate,
            endDate,
            subscription.getPlanType().name(),
            activeUsers,
            subscription.getPricePerUser(),
            totalAmount,
            subscription.getBillingCycle().name(),
            subscription.getStatus().name()
        );
    }

    @Transactional
    public boolean processPayment(@NonNull UUID companyId, BigDecimal amount) {
        logger.info("Processando pagamento de R$ {} para empresa {}", amount, companyId);

        CompanySubscription subscription = subscriptionRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura", companyId));

        if (!authorizationService.canAccessCompany(companyId)) {
            throw new AccessDeniedException("Empresa", companyId, "Usuário não tem acesso a esta empresa");
        }

        BigDecimal expectedAmount = subscriptionService.calculateMonthlyBill(companyId);

        if (amount.compareTo(expectedAmount) < 0) {
            logger.warn("Valor pago (R$ {}) menor que o esperado (R$ {})", amount, expectedAmount);

        }

        subscription.setStatus(SubscriptionStatus.ACTIVE);

        if (subscription.getBillingCycle().name().equals("MONTHLY")) {
            subscription.setNextBillingDate(LocalDateTime.now().plusMonths(1));
        } else {
            subscription.setNextBillingDate(LocalDateTime.now().plusYears(1));
        }

        subscriptionRepository.save(subscription);

        logger.info("Pagamento processado com sucesso. Próxima cobrança: {}", subscription.getNextBillingDate());
        return true;
    }

    @Transactional
    public boolean processPaymentWithHistory(@NonNull UUID companyId, BigDecimal amount, String stripeInvoiceId) {
        boolean processed = processPayment(companyId, amount);

        if (processed) {

            logger.info("Payment processed, consider recording in payment history");
        }

        return processed;
    }

    public static class InvoiceDTO {
        private final UUID companyId;
        private final String companyName;
        private final String period;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final String planType;
        private final int activeUsers;
        private final BigDecimal pricePerUser;
        private final BigDecimal totalAmount;
        private final String billingCycle;
        private final String status;

        public InvoiceDTO(UUID companyId, String companyName, String period, LocalDate startDate,
                          LocalDate endDate, String planType, int activeUsers, BigDecimal pricePerUser,
                          BigDecimal totalAmount, String billingCycle, String status) {
            this.companyId = companyId;
            this.companyName = companyName;
            this.period = period;
            this.startDate = startDate;
            this.endDate = endDate;
            this.planType = planType;
            this.activeUsers = activeUsers;
            this.pricePerUser = pricePerUser;
            this.totalAmount = totalAmount;
            this.billingCycle = billingCycle;
            this.status = status;
        }

        public UUID getCompanyId() {
            return companyId;
        }

        public String getCompanyName() {
            return companyName;
        }

        public String getPeriod() {
            return period;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public String getPlanType() {
            return planType;
        }

        public int getActiveUsers() {
            return activeUsers;
        }

        public BigDecimal getPricePerUser() {
            return pricePerUser;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public String getBillingCycle() {
            return billingCycle;
        }

        public String getStatus() {
            return status;
        }
    }
}
