package br.jeanjacintho.tideflow.user_service.service;

import br.jeanjacintho.tideflow.user_service.exception.AccessDeniedException;
import br.jeanjacintho.tideflow.user_service.exception.ResourceNotFoundException;
import br.jeanjacintho.tideflow.user_service.model.BillingCycle;
import br.jeanjacintho.tideflow.user_service.model.Company;
import br.jeanjacintho.tideflow.user_service.model.CompanySubscription;
import br.jeanjacintho.tideflow.user_service.model.SubscriptionPlan;
import br.jeanjacintho.tideflow.user_service.model.SubscriptionStatus;
import br.jeanjacintho.tideflow.user_service.repository.CompanyRepository;
import br.jeanjacintho.tideflow.user_service.repository.CompanySubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class SubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

    private static final BigDecimal FREE_PLAN_PRICE = BigDecimal.ZERO;
    private static final int FREE_PLAN_MAX_EMPLOYEES = 7;
    private static final int ENTERPRISE_PLAN_MAX_EMPLOYEES = Integer.MAX_VALUE;

    private final CompanySubscriptionRepository subscriptionRepository;
    private final CompanyRepository companyRepository;
    private final CompanyAuthorizationService authorizationService;
    private final UsageTrackingService usageTrackingService;

    @Value("${tideflow.subscription.price-brl:19990}")
    private Long subscriptionPriceCents;

    @Autowired
    public SubscriptionService(
            CompanySubscriptionRepository subscriptionRepository,
            CompanyRepository companyRepository,
            CompanyAuthorizationService authorizationService,
            UsageTrackingService usageTrackingService) {
        this.subscriptionRepository = subscriptionRepository;
        this.companyRepository = companyRepository;
        this.authorizationService = authorizationService;
        this.usageTrackingService = usageTrackingService;
    }

    private BigDecimal getEnterprisePlanPrice() {
        if (subscriptionPriceCents == null) {
            return new BigDecimal("199.90");
        }
        return BigDecimal.valueOf(subscriptionPriceCents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Transactional
    public CompanySubscription createSubscription(@NonNull UUID companyId, SubscriptionPlan planType) {
        logger.info("Criando assinatura para empresa {} com plano {}", companyId, planType);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", companyId));

        if (!authorizationService.canAccessCompany(companyId)) {
            throw new AccessDeniedException("Empresa", companyId, "Usuário não tem acesso a esta empresa");
        }

        if (subscriptionRepository.findByCompanyId(companyId).isPresent()) {
            throw new IllegalArgumentException("Empresa já possui uma assinatura ativa");
        }

        BigDecimal pricePerUser = planType == SubscriptionPlan.FREE
            ? FREE_PLAN_PRICE
            : getEnterprisePlanPrice();

        int maxEmployees = planType == SubscriptionPlan.FREE
            ? FREE_PLAN_MAX_EMPLOYEES
            : ENTERPRISE_PLAN_MAX_EMPLOYEES;

        int currentUserCount = usageTrackingService.getActiveUserCount(companyId);

        CompanySubscription subscription = new CompanySubscription(
            company,
            planType,
            pricePerUser,
            currentUserCount,
            BillingCycle.MONTHLY,
            LocalDateTime.now().plusMonths(1)
        );
        subscription.setStatus(SubscriptionStatus.TRIAL);

        CompanySubscription saved = subscriptionRepository.save(subscription);

        company.setSubscriptionPlan(planType);
        company.setMaxEmployees(maxEmployees);
        companyRepository.save(company);

        logger.info("Assinatura criada com sucesso: {}", saved.getId());
        return saved;
    }

    @Transactional
    public CompanySubscription upgradeSubscription(@NonNull UUID companyId, SubscriptionPlan newPlan) {
        logger.info("Fazendo upgrade da assinatura da empresa {} para plano {}", companyId, newPlan);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", companyId));

        if (!authorizationService.canAccessCompany(companyId)) {
            throw new AccessDeniedException("Empresa", companyId, "Usuário não tem acesso a esta empresa");
        }

        CompanySubscription subscription = subscriptionRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura", companyId));

        if (subscription.getPlanType() == newPlan) {
            throw new IllegalArgumentException("Empresa já possui o plano " + newPlan);
        }

        BigDecimal newPricePerUser = newPlan == SubscriptionPlan.FREE
            ? FREE_PLAN_PRICE
            : getEnterprisePlanPrice();

        int maxEmployees = newPlan == SubscriptionPlan.FREE
            ? FREE_PLAN_MAX_EMPLOYEES
            : ENTERPRISE_PLAN_MAX_EMPLOYEES;

        subscription.setPlanType(newPlan);
        subscription.setPricePerUser(newPricePerUser);
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        CompanySubscription saved = subscriptionRepository.save(subscription);

        company.setSubscriptionPlan(newPlan);
        company.setMaxEmployees(maxEmployees);
        companyRepository.save(company);

        logger.info("Upgrade realizado com sucesso para plano {}", newPlan);
        return saved;
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateMonthlyBill(@NonNull UUID companyId) {
        logger.info("Calculando fatura mensal para empresa {}", companyId);

        CompanySubscription subscription = subscriptionRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura", companyId));

        if (!authorizationService.canAccessCompany(companyId)) {
            throw new AccessDeniedException("Empresa", companyId, "Usuário não tem acesso a esta empresa");
        }

        int activeUsers = usageTrackingService.getActiveUserCount(companyId);

        if (subscription.getPlanType() == SubscriptionPlan.FREE) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalBill = subscription.getPricePerUser()
                .multiply(new BigDecimal(activeUsers))
                .setScale(2, RoundingMode.HALF_UP);

        logger.info("Fatura mensal calculada: R$ {} para {} usuários", totalBill, activeUsers);
        return totalBill;
    }

    @Transactional
    public void updateUserCount(@NonNull UUID companyId) {
        CompanySubscription subscription = subscriptionRepository.findByCompanyId(companyId)
                .orElse(null);

        if (subscription != null) {
            int activeUsers = usageTrackingService.getActiveUserCount(companyId);
            subscription.setTotalUsers(activeUsers);
            subscriptionRepository.save(subscription);
        }
    }

    @Transactional(readOnly = true)
    public boolean canAddUsers(@NonNull UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", companyId));

        int activeUsers = usageTrackingService.getActiveUserCount(companyId);
        return activeUsers < company.getMaxEmployees();
    }

    @Transactional(readOnly = true)
    public CompanySubscription getSubscription(@NonNull UUID companyId) {
        return subscriptionRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura", companyId));
    }

    @Transactional
    public CompanySubscription updateSubscription(@NonNull CompanySubscription subscription) {
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public void suspendSubscription(@NonNull UUID companyId) {
        logger.info("Suspendendo assinatura da empresa: {}", companyId);

        CompanySubscription subscription = subscriptionRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura", companyId));

        subscription.setStatus(SubscriptionStatus.SUSPENDED);
        subscriptionRepository.save(subscription);

        logger.info("Assinatura suspensa com sucesso.");
    }

    @Transactional
    public void activateStripeSubscription(@NonNull UUID companyId, @NonNull String stripeCustomerId, @NonNull com.stripe.model.Subscription stripeSubscription) {
        logger.info("Activating Stripe subscription for company: {} with subscription: {}", companyId, stripeSubscription.getId());

        CompanySubscription subscription = subscriptionRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura", companyId));

        subscription.setStripeCustomerId(stripeCustomerId);
        subscription.setStripeSubscriptionId(stripeSubscription.getId());
        subscription.setPlanType(SubscriptionPlan.ENTERPRISE);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setPricePerUser(getEnterprisePlanPrice());

        Long currentPeriodEnd = stripeSubscription.getCurrentPeriodEnd();
        Long trialEnd = stripeSubscription.getTrialEnd();
        String stripeStatus = stripeSubscription.getStatus();

        if ("trialing".equals(stripeStatus) && trialEnd != null) {
            LocalDateTime nextBilling = Instant.ofEpochSecond(trialEnd)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            subscription.setNextBillingDate(nextBilling);
            logger.info("Next billing date set from trial_end (trialing status): {}", nextBilling);
        } else if (trialEnd != null && currentPeriodEnd != null) {

            LocalDateTime trialEndDate = Instant.ofEpochSecond(trialEnd)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            LocalDateTime currentPeriodEndDate = Instant.ofEpochSecond(currentPeriodEnd)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            LocalDateTime nextBilling = trialEndDate.isAfter(currentPeriodEndDate) ? trialEndDate : currentPeriodEndDate;
            subscription.setNextBillingDate(nextBilling);
            logger.info("Next billing date set from {} (trial_end: {}, current_period_end: {})",
                    nextBilling, trialEndDate, currentPeriodEndDate);
        } else if (currentPeriodEnd != null) {
            LocalDateTime nextBilling = Instant.ofEpochSecond(currentPeriodEnd)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            subscription.setNextBillingDate(nextBilling);
            logger.info("Next billing date set from current_period_end: {}", nextBilling);
        } else if (trialEnd != null) {
            LocalDateTime nextBilling = Instant.ofEpochSecond(trialEnd)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            subscription.setNextBillingDate(nextBilling);
            logger.info("Next billing date set from trial_end: {}", nextBilling);
        } else {

            if (subscription.getBillingCycle() == BillingCycle.MONTHLY) {
                subscription.setNextBillingDate(LocalDateTime.now().plusMonths(1));
            } else {
                subscription.setNextBillingDate(LocalDateTime.now().plusYears(1));
            }
            logger.info("Next billing date calculated from billing cycle: {}", subscription.getNextBillingDate());
        }

        if (!stripeSubscription.getItems().getData().isEmpty()) {
            String priceId = stripeSubscription.getItems().getData().get(0).getPrice().getId();
            subscription.setStripePriceId(priceId);

            Long quantity = stripeSubscription.getItems().getData().get(0).getQuantity();
            if (quantity != null) {
                subscription.setTotalUsers(quantity.intValue());
            }
        }

        Company company = subscription.getCompany();
        company.setSubscriptionPlan(SubscriptionPlan.ENTERPRISE);
        company.setMaxEmployees(ENTERPRISE_PLAN_MAX_EMPLOYEES);
        companyRepository.save(company);

        subscriptionRepository.save(subscription);
        logger.info("Stripe subscription activated successfully - Plan: {}, Status: {}, Next Billing: {}",
                subscription.getPlanType(), subscription.getStatus(), subscription.getNextBillingDate());
    }

    @Transactional
    public void activateStripeSubscription(@NonNull UUID companyId, @NonNull String stripeCustomerId, @NonNull String stripeSubscriptionId) {
        logger.info("Activating Stripe subscription for company: {} with subscription ID: {}", companyId, stripeSubscriptionId);

        CompanySubscription subscription = subscriptionRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura", companyId));

        subscription.setStripeCustomerId(stripeCustomerId);
        subscription.setStripeSubscriptionId(stripeSubscriptionId);
        subscription.setPlanType(SubscriptionPlan.ENTERPRISE);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setPricePerUser(getEnterprisePlanPrice());

        Company company = subscription.getCompany();
        company.setSubscriptionPlan(SubscriptionPlan.ENTERPRISE);
        company.setMaxEmployees(ENTERPRISE_PLAN_MAX_EMPLOYEES);
        companyRepository.save(company);

        subscriptionRepository.save(subscription);
        logger.info("Stripe subscription activated successfully");
    }

    @Transactional
    public void syncSubscriptionFromStripe(@NonNull String customerId, com.stripe.model.Subscription stripeSubscription) {
        logger.info("Syncing subscription from Stripe - customerId: {}", customerId);

        CompanySubscription subscription = subscriptionRepository.findByStripeCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura", "customerId: " + customerId));

        subscription.setStripeSubscriptionId(stripeSubscription.getId());

        if (!stripeSubscription.getItems().getData().isEmpty()) {
            com.stripe.model.SubscriptionItem item = stripeSubscription.getItems().getData().get(0);

            Long quantity = item.getQuantity();
            if (quantity != null) {
                subscription.setTotalUsers(quantity.intValue());
            }

            String priceId = item.getPrice().getId();
            subscription.setStripePriceId(priceId);

            if (priceId != null && !priceId.isEmpty()) {
                subscription.setPlanType(SubscriptionPlan.ENTERPRISE);
                subscription.setPricePerUser(getEnterprisePlanPrice());

                Company company = subscription.getCompany();
                if (company.getSubscriptionPlan() != SubscriptionPlan.ENTERPRISE) {
                    company.setSubscriptionPlan(SubscriptionPlan.ENTERPRISE);
                    company.setMaxEmployees(ENTERPRISE_PLAN_MAX_EMPLOYEES);
                    companyRepository.save(company);
                    logger.info("Company plan updated to ENTERPRISE via sync");
                }
            }
        }

        Long currentPeriodEnd = stripeSubscription.getCurrentPeriodEnd();
        Long trialEnd = stripeSubscription.getTrialEnd();
        String stripeStatus = stripeSubscription.getStatus();

        if ("trialing".equals(stripeStatus) && trialEnd != null) {
            LocalDateTime nextBilling = Instant.ofEpochSecond(trialEnd)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            subscription.setNextBillingDate(nextBilling);
            logger.info("Next billing date synced from trial_end (trialing status): {}", nextBilling);
        } else if (trialEnd != null && currentPeriodEnd != null) {

            LocalDateTime trialEndDate = Instant.ofEpochSecond(trialEnd)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            LocalDateTime currentPeriodEndDate = Instant.ofEpochSecond(currentPeriodEnd)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            LocalDateTime nextBilling = trialEndDate.isAfter(currentPeriodEndDate) ? trialEndDate : currentPeriodEndDate;
            subscription.setNextBillingDate(nextBilling);
            logger.info("Next billing date synced from {} (trial_end: {}, current_period_end: {})",
                    nextBilling, trialEndDate, currentPeriodEndDate);
        } else if (currentPeriodEnd != null) {
            LocalDateTime nextBilling = Instant.ofEpochSecond(currentPeriodEnd)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            subscription.setNextBillingDate(nextBilling);
            logger.info("Next billing date synced from current_period_end: {}", nextBilling);
        } else if (trialEnd != null) {
            LocalDateTime nextBilling = Instant.ofEpochSecond(trialEnd)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            subscription.setNextBillingDate(nextBilling);
            logger.info("Next billing date synced from trial_end: {}", nextBilling);
        }

        if ("active".equals(stripeStatus) || "trialing".equals(stripeStatus)) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
        } else if ("past_due".equals(stripeStatus) || "unpaid".equals(stripeStatus)) {
            subscription.setStatus(SubscriptionStatus.SUSPENDED);
        } else if ("canceled".equals(stripeStatus) || "incomplete_expired".equals(stripeStatus)) {
            subscription.setStatus(SubscriptionStatus.CANCELLED);
        }

        subscriptionRepository.save(subscription);
        logger.info("Subscription synced successfully - Plan: {}, Status: {}, Next Billing: {}",
                subscription.getPlanType(), subscription.getStatus(), subscription.getNextBillingDate());
    }

    @Transactional
    public void cancelStripeSubscription(@NonNull String customerId) {
        logger.info("Canceling subscription for customerId: {}", customerId);

        CompanySubscription subscription = subscriptionRepository.findByStripeCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura", "customerId: " + customerId));

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setPlanType(SubscriptionPlan.FREE);
        subscription.setPricePerUser(FREE_PLAN_PRICE);

        Company company = subscription.getCompany();
        company.setSubscriptionPlan(SubscriptionPlan.FREE);
        company.setMaxEmployees(FREE_PLAN_MAX_EMPLOYEES);
        companyRepository.save(company);

        subscriptionRepository.save(subscription);
        logger.info("Subscription canceled successfully");
    }
}
