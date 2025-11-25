package br.jeanjacintho.tideflow.user_service.service;

import br.jeanjacintho.tideflow.user_service.exception.ResourceNotFoundException;
import br.jeanjacintho.tideflow.user_service.model.Company;
import br.jeanjacintho.tideflow.user_service.repository.CompanyRepository;
import br.jeanjacintho.tideflow.user_service.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UsageTrackingService {

    private static final Logger logger = LoggerFactory.getLogger(UsageTrackingService.class);

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @Autowired
    public UsageTrackingService(UserRepository userRepository, CompanyRepository companyRepository) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
    }

    /**
     * Conta o número de usuários ativos de uma empresa.
     * 
     * @param companyId ID da empresa
     * @return Número de usuários ativos
     */
    public int getActiveUserCount(@NonNull UUID companyId) {
        logger.debug("Contando usuários ativos da empresa {}", companyId);
        
        long count = userRepository.countActiveUsersByCompanyId(companyId);
        logger.debug("Empresa {} possui {} usuários ativos", companyId, count);
        return (int) count;
    }

    /**
     * Verifica se a empresa está dentro dos limites do plano.
     * 
     * @param companyId ID da empresa
     * @return true se está dentro dos limites, false caso contrário
     */
    @Transactional(readOnly = true)
    public boolean checkUsageLimits(@NonNull UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", companyId));

        int activeUsers = getActiveUserCount(companyId);
        boolean withinLimits = activeUsers < company.getMaxEmployees();

        if (!withinLimits) {
            logger.warn("Empresa {} excedeu o limite de usuários: {}/{}", 
                companyId, activeUsers, company.getMaxEmployees());
        }

        return withinLimits;
    }

    /**
     * Verifica se a empresa pode adicionar mais usuários e retorna mensagem de erro se não puder.
     * Verifica se após adicionar 1 usuário, o limite será excedido.
     * 
     * @param companyId ID da empresa
     * @throws IllegalArgumentException se o limite foi atingido
     */
    public void validateUsageLimits(@NonNull UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", companyId));

        int activeUsers = getActiveUserCount(companyId);
        
        // Sincroniza o maxEmployees com o plano atual da empresa
        int maxEmployees = getMaxEmployeesForPlan(company.getSubscriptionPlan());
        
        // Se o valor no banco está diferente, atualiza
        Integer currentMaxEmployees = company.getMaxEmployees();
        if (currentMaxEmployees == null || currentMaxEmployees.intValue() != maxEmployees) {
            logger.info("Sincronizando maxEmployees da empresa {}: {} -> {}", 
                companyId, currentMaxEmployees, maxEmployees);
            company.setMaxEmployees(maxEmployees);
            companyRepository.save(company);
        }
        
        logger.info("Validando limite de usuários para empresa {}: {} usuários ativos de {} permitidos", 
            companyId, activeUsers, maxEmployees);
        
        // Verifica se após adicionar 1 usuário, o limite será excedido
        if (activeUsers + 1 > maxEmployees) {
            String message = String.format(
                "Limite de usuários atingido. Plano atual permite %d usuários. Você possui %d usuários ativos. Upgrade necessário para adicionar mais usuários.",
                maxEmployees, activeUsers
            );
            logger.error("Limite de usuários excedido para empresa {}: {} usuários ativos, limite: {}, tentando adicionar mais 1", 
                companyId, activeUsers, maxEmployees);
            throw new IllegalArgumentException(message);
        }
        
        logger.debug("Validação de limite aprovada para empresa {}: {} usuários ativos, limite: {}", 
            companyId, activeUsers, maxEmployees);
    }
    
    /**
     * Retorna o número máximo de funcionários permitidos para um plano.
     * 
     * @param plan Plano de assinatura
     * @return Número máximo de funcionários
     */
    private int getMaxEmployeesForPlan(br.jeanjacintho.tideflow.user_service.model.SubscriptionPlan plan) {
        if (plan == null) {
            return 7; // Default para FREE
        }
        
        switch (plan) {
            case FREE:
                return 7;
            case ENTERPRISE:
                return Integer.MAX_VALUE;
            default:
                return 7;
        }
    }

    /**
     * Retorna informações sobre o uso atual da empresa.
     * 
     * @param companyId ID da empresa
     * @return Informações de uso
     */
    @Transactional(readOnly = true)
    public UsageInfo getUsageInfo(@NonNull UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", companyId));

        int activeUsers = getActiveUserCount(companyId);
        
        // Sincroniza o maxEmployees com o plano atual da empresa
        int maxUsers = getMaxEmployeesForPlan(company.getSubscriptionPlan());
        
        // Se o valor no banco está diferente, atualiza (mas não commitamos em readOnly, apenas usamos o valor correto)
        Integer currentMaxEmployees = company.getMaxEmployees();
        if (currentMaxEmployees == null || currentMaxEmployees.intValue() != maxUsers) {
            logger.debug("Valor de maxEmployees desatualizado para empresa {}: {} (banco) vs {} (plano)", 
                companyId, currentMaxEmployees, maxUsers);
        }
        
        boolean atLimit = activeUsers >= maxUsers;
        int remainingSlots = Math.max(0, maxUsers - activeUsers);

        return new UsageInfo(activeUsers, maxUsers, atLimit, remainingSlots);
    }

    /**
     * Classe para retornar informações de uso.
     */
    public static class UsageInfo {
        private final int activeUsers;
        private final int maxUsers;
        private final boolean atLimit;
        private final int remainingSlots;

        public UsageInfo(int activeUsers, int maxUsers, boolean atLimit, int remainingSlots) {
            this.activeUsers = activeUsers;
            this.maxUsers = maxUsers;
            this.atLimit = atLimit;
            this.remainingSlots = remainingSlots;
        }

        public int getActiveUsers() {
            return activeUsers;
        }

        public int getMaxUsers() {
            return maxUsers;
        }

        public boolean isAtLimit() {
            return atLimit;
        }

        public int getRemainingSlots() {
            return remainingSlots;
        }
    }
}
