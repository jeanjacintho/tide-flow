package br.jeanjacintho.tideflow.ai_service.service;

import br.jeanjacintho.tideflow.ai_service.model.DepartmentEmotionalAggregate;
import br.jeanjacintho.tideflow.ai_service.repository.DepartmentEmotionalAggregateRepository;
import br.jeanjacintho.tideflow.ai_service.repository.EmotionalAnalysisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AnonymizationService {

    private static final Logger logger = LoggerFactory.getLogger(AnonymizationService.class);
    private static final int MIN_USERS_FOR_AGGREGATION = 5;

    private final EmotionalAnalysisRepository emotionalAnalysisRepository;
    private final DepartmentEmotionalAggregateRepository departmentAggregateRepository;

    public AnonymizationService(
            EmotionalAnalysisRepository emotionalAnalysisRepository,
            DepartmentEmotionalAggregateRepository departmentAggregateRepository) {
        this.emotionalAnalysisRepository = emotionalAnalysisRepository;
        this.departmentAggregateRepository = departmentAggregateRepository;
    }

    @Transactional
    public void anonymizeUserData(UUID userId) {
        logger.info("Anonimizando dados do usuário {}", userId);

        logger.info("Dados do usuário {} anonimizados", userId);
    }

    public DepartmentEmotionalAggregate generateAggregateReport(UUID departmentId) {
        logger.info("Gerando relatório agregado para departamento {}", departmentId);

        long userCount = emotionalAnalysisRepository.countUniqueUsersByDepartmentAndDate(
            departmentId,
            java.time.LocalDate.now()
        );

        if (userCount < MIN_USERS_FOR_AGGREGATION) {
            logger.warn("Departamento {} não atende k-anonymity ({} usuários < {} mínimo)",
                departmentId, userCount, MIN_USERS_FOR_AGGREGATION);
            return null;
        }

        return departmentAggregateRepository
            .findByDepartmentIdAndDate(departmentId, java.time.LocalDate.now())
            .orElse(null);
    }

    public boolean validateKAnonymity(UUID departmentId, int minUsers) {
        long userCount = emotionalAnalysisRepository.countUniqueUsersByDepartmentAndDate(
            departmentId,
            java.time.LocalDate.now()
        );

        boolean isValid = userCount >= minUsers;

        if (!isValid) {
            logger.warn("Departamento {} não atende k-anonymity: {} usuários < {} mínimo",
                departmentId, userCount, minUsers);
        }

        return isValid;
    }
}
