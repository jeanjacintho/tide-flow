package br.jeanjacintho.tideflow.ai_service.service;

import br.jeanjacintho.tideflow.ai_service.dto.response.TurnoverPredictionDTO;
import br.jeanjacintho.tideflow.ai_service.model.DepartmentEmotionalAggregate;
import br.jeanjacintho.tideflow.ai_service.repository.DepartmentEmotionalAggregateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TurnoverPredictionService {

    private static final Logger logger = LoggerFactory.getLogger(TurnoverPredictionService.class);
    
    private static final int DAYS_TO_ANALYZE = 30;
    private static final double HIGH_STRESS_THRESHOLD = 70.0;
    private static final double LOW_ENGAGEMENT_THRESHOLD = 40.0;

    private final DepartmentEmotionalAggregateRepository departmentAggregateRepository;

    public TurnoverPredictionService(DepartmentEmotionalAggregateRepository departmentAggregateRepository) {
        this.departmentAggregateRepository = departmentAggregateRepository;
    }

    /**
     * Prediz risco de turnover para um departamento.
     * 
     * @param companyId ID da empresa
     * @param departmentId ID do departamento (opcional, se null analisa toda a empresa)
     * @return TurnoverPredictionDTO com score de risco e probabilidades
     */
    @Transactional(readOnly = true)
    public TurnoverPredictionDTO predictTurnoverRisk(UUID companyId, UUID departmentId) {
        logger.info("Predizendo risco de turnover para empresa {} e departamento {}", companyId, departmentId);
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(DAYS_TO_ANALYZE);
        
        List<DepartmentEmotionalAggregate> aggregates;
        if (departmentId != null) {
            aggregates = departmentAggregateRepository.findByDepartmentIdAndDateBetween(
                departmentId, startDate, endDate
            );
        } else {
            aggregates = departmentAggregateRepository.findByCompanyIdAndDateBetween(
                companyId, startDate, endDate
            );
        }
        
        if (aggregates.isEmpty()) {
            logger.warn("Nenhuma agregação encontrada para análise de turnover");
            return createEmptyPrediction(companyId, departmentId, null);
        }

        int riskScore = calculateRiskScore(aggregates);
        String riskLevel = determineRiskLevel(riskScore);
        List<TurnoverPredictionDTO.TurnoverProbability> probabilities = calculateProbabilities(riskScore);
        List<TurnoverPredictionDTO.RiskFactor> riskFactors = identifyRiskFactors(aggregates);
        List<String> recommendations = generateRecommendations(riskScore, riskFactors);

        return new TurnoverPredictionDTO(
            companyId,
            departmentId,
            null, // Nome do departamento seria buscado do user-service
            riskScore,
            riskLevel,
            probabilities,
            riskFactors,
            recommendations
        );
    }

    /**
     * Calcula score de risco (0-100) baseado em múltiplos fatores.
     */
    private int calculateRiskScore(List<DepartmentEmotionalAggregate> aggregates) {
        double avgStress = aggregates.stream()
            .filter(a -> a.getAvgStressLevel() != null)
            .mapToDouble(DepartmentEmotionalAggregate::getAvgStressLevel)
            .average()
            .orElse(50.0);
        
        double avgIntensity = aggregates.stream()
            .filter(a -> a.getAvgEmotionalIntensity() != null)
            .mapToDouble(DepartmentEmotionalAggregate::getAvgEmotionalIntensity)
            .average()
            .orElse(50.0);
        
        long totalRiskAlerts = aggregates.stream()
            .filter(a -> a.getRiskAlertsCount() != null)
            .mapToLong(DepartmentEmotionalAggregate::getRiskAlertsCount)
            .sum();
        
        long totalConversations = aggregates.stream()
            .filter(a -> a.getTotalConversations() != null)
            .mapToLong(DepartmentEmotionalAggregate::getTotalConversations)
            .sum();
        
        double riskFromStress = Math.min(100, avgStress * 1.2);
        double riskFromIntensity = Math.min(100, avgIntensity * 1.1);
        double riskFromAlerts = Math.min(100, (totalRiskAlerts / Math.max(1.0, totalConversations)) * 100);
        
        double engagementScore = calculateEngagementScore(aggregates);
        double riskFromEngagement = Math.max(0, 100 - engagementScore);
        
        int riskScore = (int) Math.round(
            (riskFromStress * 0.35) +
            (riskFromIntensity * 0.25) +
            (riskFromAlerts * 0.20) +
            (riskFromEngagement * 0.20)
        );
        
        return Math.max(0, Math.min(100, riskScore));
    }

    /**
     * Calcula score de engajamento baseado em conversas e atividade.
     */
    private double calculateEngagementScore(List<DepartmentEmotionalAggregate> aggregates) {
        if (aggregates.isEmpty()) {
            return 50.0;
        }
        
        double avgConversations = aggregates.stream()
            .filter(a -> a.getTotalConversations() != null)
            .mapToLong(DepartmentEmotionalAggregate::getTotalConversations)
            .average()
            .orElse(0.0);
        
        double avgUsers = aggregates.stream()
            .filter(a -> a.getUniqueUsersCount() != null)
            .mapToLong(DepartmentEmotionalAggregate::getUniqueUsersCount)
            .average()
            .orElse(0.0);
        
        double engagement = (avgConversations / Math.max(1.0, avgUsers)) * 10;
        return Math.min(100, engagement);
    }

    /**
     * Determina nível de risco baseado no score.
     */
    private String determineRiskLevel(int riskScore) {
        if (riskScore >= 80) {
            return "CRITICAL";
        } else if (riskScore >= 60) {
            return "HIGH";
        } else if (riskScore >= 40) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    /**
     * Calcula probabilidades de turnover em 30, 60 e 90 dias.
     */
    private List<TurnoverPredictionDTO.TurnoverProbability> calculateProbabilities(int riskScore) {
        List<TurnoverPredictionDTO.TurnoverProbability> probabilities = new ArrayList<>();
        
        double baseProbability = riskScore / 100.0;
        
        probabilities.add(new TurnoverPredictionDTO.TurnoverProbability(
            30,
            Math.min(100.0, baseProbability * 30)
        ));
        
        probabilities.add(new TurnoverPredictionDTO.TurnoverProbability(
            60,
            Math.min(100.0, baseProbability * 50)
        ));
        
        probabilities.add(new TurnoverPredictionDTO.TurnoverProbability(
            90,
            Math.min(100.0, baseProbability * 70)
        ));
        
        return probabilities;
    }

    /**
     * Identifica fatores de risco.
     */
    private List<TurnoverPredictionDTO.RiskFactor> identifyRiskFactors(List<DepartmentEmotionalAggregate> aggregates) {
        List<TurnoverPredictionDTO.RiskFactor> factors = new ArrayList<>();
        
        double avgStress = aggregates.stream()
            .filter(a -> a.getAvgStressLevel() != null)
            .mapToDouble(DepartmentEmotionalAggregate::getAvgStressLevel)
            .average()
            .orElse(50.0);
        
        if (avgStress >= HIGH_STRESS_THRESHOLD) {
            factors.add(new TurnoverPredictionDTO.RiskFactor(
                "HIGH_STRESS",
                "Nível de stress consistentemente alto",
                80,
                0.35
            ));
        }
        
        double engagement = calculateEngagementScore(aggregates);
        if (engagement <= LOW_ENGAGEMENT_THRESHOLD) {
            factors.add(new TurnoverPredictionDTO.RiskFactor(
                "LOW_ENGAGEMENT",
                "Baixo engajamento e participação",
                70,
                0.20
            ));
        }
        
        long totalRiskAlerts = aggregates.stream()
            .filter(a -> a.getRiskAlertsCount() != null)
            .mapToLong(DepartmentEmotionalAggregate::getRiskAlertsCount)
            .sum();
        
        if (totalRiskAlerts > aggregates.size() * 2) {
            factors.add(new TurnoverPredictionDTO.RiskFactor(
                "FREQUENT_RISK_ALERTS",
                "Muitos alertas de risco emocional",
                60,
                0.20
            ));
        }
        
        return factors;
    }

    /**
     * Gera recomendações baseadas no score de risco e fatores identificados.
     */
    private List<String> generateRecommendations(int riskScore, List<TurnoverPredictionDTO.RiskFactor> riskFactors) {
        List<String> recommendations = new ArrayList<>();
        
        if (riskScore >= 80) {
            recommendations.add("Ação imediata necessária: Intervenção urgente recomendada");
            recommendations.add("Agendar reunião com equipe para entender causas do stress");
            recommendations.add("Considerar suporte psicológico adicional");
        } else if (riskScore >= 60) {
            recommendations.add("Monitoramento próximo recomendado");
            recommendations.add("Implementar ações preventivas de retenção");
            recommendations.add("Avaliar carga de trabalho e prazos");
        } else if (riskScore >= 40) {
            recommendations.add("Manter atenção aos sinais de desengajamento");
            recommendations.add("Fortalecer comunicação e feedback");
        } else {
            recommendations.add("Situação estável, manter práticas atuais");
        }
        
        return recommendations;
    }

    private TurnoverPredictionDTO createEmptyPrediction(UUID companyId, UUID departmentId, String departmentName) {
        return new TurnoverPredictionDTO(
            companyId,
            departmentId,
            departmentName,
            0,
            "LOW",
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>()
        );
    }
}
