package br.jeanjacintho.tideflow.user_service.service;

import br.jeanjacintho.tideflow.user_service.config.TenantContext;
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
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class SubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);
    
    private static final BigDecimal FREE_PLAN_PRICE = BigDecimal.ZERO;
    private static final BigDecimal ENTERPRISE_PLAN_PRICE_PER_USER = new BigDecimal("6.00"); // €6 por usuário/mês
    private static final int FREE_PLAN_MAX_EMPLOYEES = 20;
    private static final int ENTERPRISE_PLAN_MAX_EMPLOYEES = Integer.MAX_VALUE;

    private final CompanySubscriptionRepository subscriptionRepository;
    private final CompanyRepository companyRepository;
    private final CompanyAuthorizationService authorizationService;
    private final UsageTrackingService usageTrackingService;

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

    /**
     * Cria uma assinatura para uma empresa.
     * 
     * @param companyId ID da empresa
     * @param planType Tipo de plano (FREE ou ENTERPRISE)
     * @return CompanySubscription criada
     */
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
            : ENTERPRISE_PLAN_PRICE_PER_USER;

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
            LocalDate.now().plusMonths(1)
        );

        subscription.setStatus(SubscriptionStatus.TRIAL);
        
        CompanySubscription saved = subscriptionRepository.save(subscription);
        
        company.setSubscriptionPlan(planType);
        company.setMaxEmployees(maxEmployees);
        companyRepository.save(company);

        logger.info("Assinatura criada com sucesso: {}", saved.getId());
        return saved;
    }

    /**
     * Faz upgrade da assinatura de uma empresa.
     * 
     * @param companyId ID da empresa
     * @param newPlan Novo plano (FREE ou ENTERPRISE)
     * @return CompanySubscription atualizada
     */
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
            : ENTERPRISE_PLAN_PRICE_PER_USER;

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

    /**
     * Calcula a fatura mensal (PMPM - Per Member Per Month) de uma empresa.
     * 
     * @param companyId ID da empresa
     * @return Valor total da fatura mensal
     */
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

        logger.info("Fatura mensal calculada: €{} para {} usuários", totalBill, activeUsers);
        return totalBill;
    }

    /**
     * Atualiza o número de usuários na assinatura.
     */
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

    /**
     * Verifica se a empresa pode adicionar mais usuários.
     */
    @Transactional(readOnly = true)
    public boolean canAddUsers(@NonNull UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", companyId));

        int activeUsers = usageTrackingService.getActiveUserCount(companyId);
        return activeUsers < company.getMaxEmployees();
    }

    /**
     * Obtém a assinatura de uma empresa.
     */
    @Transactional(readOnly = true)
    public CompanySubscription getSubscription(@NonNull UUID companyId) {
        return subscriptionRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura", companyId));
    }
}
