package br.jeanjacintho.tideflow.ai_service.service;

import br.jeanjacintho.tideflow.ai_service.model.EmotionalAnalysis;
import br.jeanjacintho.tideflow.ai_service.repository.EmotionalAnalysisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DepartmentTriggerAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(DepartmentTriggerAnalysisService.class);

    private static final int MIN_TRIGGERS_TO_ANALYZE = 3;
    private static final int MAX_TOP_TRIGGERS = 10;

    private final EmotionalAnalysisRepository emotionalAnalysisRepository;

    public DepartmentTriggerAnalysisService(
            EmotionalAnalysisRepository emotionalAnalysisRepository) {
        this.emotionalAnalysisRepository = emotionalAnalysisRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> aggregateTriggersByDepartment(UUID departmentId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        logger.info("Agregando triggers para departamento {} no período {} - {}",
            departmentId, startDateTime, endDateTime);

        List<EmotionalAnalysis> analyses = emotionalAnalysisRepository.findByDepartmentIdAndDateRange(
            departmentId, startDateTime, endDateTime
        );

        if (analyses.isEmpty()) {
            logger.warn("Nenhuma análise encontrada para departamento {}", departmentId);
            return new HashMap<>();
        }

        Map<String, TriggerAggregate> triggerAggregates = new HashMap<>();

        for (EmotionalAnalysis analysis : analyses) {
            if (analysis.getTriggers() != null && !analysis.getTriggers().isEmpty()) {
                for (String triggerDescription : analysis.getTriggers()) {
                    if (triggerDescription != null && !triggerDescription.trim().isEmpty()) {
                        String normalizedTrigger = normalizeTrigger(triggerDescription);

                        TriggerAggregate aggregate = triggerAggregates.computeIfAbsent(
                            normalizedTrigger,
                            k -> new TriggerAggregate(normalizedTrigger)
                        );

                        aggregate.incrementFrequency();
                        aggregate.addIntensity(analysis.getIntensity() != null ? analysis.getIntensity() : 50);
                        aggregate.addEmotion(analysis.getPrimaryEmotional());
                    }
                }
            }
        }

        return buildTopTriggersMap(triggerAggregates);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> analyzeTriggerStressCorrelation(UUID departmentId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        logger.info("Analisando correlação trigger-stress para departamento {} no período {} - {}",
            departmentId, startDateTime, endDateTime);

        List<EmotionalAnalysis> analyses = emotionalAnalysisRepository.findByDepartmentIdAndDateRange(
            departmentId, startDateTime, endDateTime
        );

        if (analyses.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, List<Integer>> triggerStressLevels = new HashMap<>();

        for (EmotionalAnalysis analysis : analyses) {
            int stressLevel = calculateStressLevel(analysis);

            if (analysis.getTriggers() != null && !analysis.getTriggers().isEmpty()) {
                for (String triggerDescription : analysis.getTriggers()) {
                    if (triggerDescription != null && !triggerDescription.trim().isEmpty()) {
                        String normalizedTrigger = normalizeTrigger(triggerDescription);
                        triggerStressLevels.computeIfAbsent(normalizedTrigger, k -> new ArrayList<>())
                            .add(stressLevel);
                    }
                }
            }
        }

        Map<String, Object> correlationMap = new HashMap<>();
        for (Map.Entry<String, List<Integer>> entry : triggerStressLevels.entrySet()) {
            if (entry.getValue().size() >= MIN_TRIGGERS_TO_ANALYZE) {
                double avgStress = entry.getValue().stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0);

                Map<String, Object> triggerData = new HashMap<>();
                triggerData.put("averageStressLevel", avgStress);
                triggerData.put("occurrences", entry.getValue().size());
                triggerData.put("correlation", calculateCorrelationStrength(avgStress));

                correlationMap.put(entry.getKey(), triggerData);
            }
        }

        return correlationMap;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> identifyStressPatternsBySector(UUID departmentId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        logger.info("Identificando padrões de stress para departamento {} no período {} - {}",
            departmentId, startDateTime, endDateTime);

        Map<String, Object> triggerAggregates = aggregateTriggersByDepartment(departmentId, startDateTime, endDateTime);
        Map<String, Object> correlationMap = analyzeTriggerStressCorrelation(departmentId, startDateTime, endDateTime);

        Map<String, Object> patterns = new HashMap<>();
        patterns.put("topTriggers", triggerAggregates);
        patterns.put("stressCorrelation", correlationMap);
        patterns.put("analysisPeriod", Map.of(
            "start", startDateTime.toString(),
            "end", endDateTime.toString()
        ));

        return patterns;
    }

    private String normalizeTrigger(String trigger) {
        return trigger.toLowerCase()
            .trim()
            .replaceAll("\\s+", " ");
    }

    private int calculateStressLevel(EmotionalAnalysis analysis) {
        String emotion = analysis.getPrimaryEmotional();
        Integer intensity = analysis.getIntensity() != null ? analysis.getIntensity() : 50;

        Map<String, Integer> emotionStressMap = Map.of(
            "ansiedade", 80,
            "estresse", 90,
            "tristeza", 60,
            "raiva", 75,
            "medo", 70,
            "frustração", 65,
            "alegria", 20,
            "felicidade", 15,
            "calma", 25,
            "paz", 20
        );

        int baseStress = emotionStressMap.getOrDefault(emotion != null ? emotion.toLowerCase() : "", 50);
        return (int) (baseStress * (intensity / 100.0));
    }

    private String calculateCorrelationStrength(double avgStress) {
        if (avgStress >= 70) {
            return "ALTA";
        } else if (avgStress >= 50) {
            return "MÉDIA";
        } else {
            return "BAIXA";
        }
    }

    private Map<String, Object> buildTopTriggersMap(Map<String, TriggerAggregate> triggerAggregates) {
        List<TriggerAggregate> sortedTriggers = triggerAggregates.values().stream()
            .sorted((a, b) -> {
                int freqCompare = Integer.compare(b.getFrequency(), a.getFrequency());
                if (freqCompare != 0) return freqCompare;
                return Double.compare(b.getAverageIntensity(), a.getAverageIntensity());
            })
            .limit(MAX_TOP_TRIGGERS)
            .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        for (TriggerAggregate aggregate : sortedTriggers) {
            Map<String, Object> triggerData = new HashMap<>();
            triggerData.put("frequency", aggregate.getFrequency());
            triggerData.put("averageIntensity", aggregate.getAverageIntensity());
            triggerData.put("primaryEmotion", aggregate.getPrimaryEmotion());
            triggerData.put("emotionDistribution", aggregate.getEmotionDistribution());

            result.put(aggregate.getTriggerDescription(), triggerData);
        }

        return result;
    }

    private static class TriggerAggregate {
        private final String triggerDescription;
        private int frequency;
        private final List<Integer> intensities;
        private final Map<String, Integer> emotionCounts;

        public TriggerAggregate(String triggerDescription) {
            this.triggerDescription = triggerDescription;
            this.frequency = 0;
            this.intensities = new ArrayList<>();
            this.emotionCounts = new HashMap<>();
        }

        public void incrementFrequency() {
            this.frequency++;
        }

        public void addIntensity(int intensity) {
            this.intensities.add(intensity);
        }

        public void addEmotion(String emotion) {
            if (emotion != null) {
                emotionCounts.merge(emotion.toLowerCase(), 1, Integer::sum);
            }
        }

        public String getTriggerDescription() {
            return triggerDescription;
        }

        public int getFrequency() {
            return frequency;
        }

        public double getAverageIntensity() {
            if (intensities.isEmpty()) {
                return 50.0;
            }
            return intensities.stream().mapToInt(Integer::intValue).average().orElse(50.0);
        }

        public String getPrimaryEmotion() {
            return emotionCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("desconhecida");
        }

        public Map<String, Integer> getEmotionDistribution() {
            return new HashMap<>(emotionCounts);
        }
    }
}
