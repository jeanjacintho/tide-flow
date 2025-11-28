package br.jeanjacintho.tideflow.ai_service.service;

import br.jeanjacintho.tideflow.ai_service.dto.response.StressTimelineDTO;
import br.jeanjacintho.tideflow.ai_service.model.CompanyEmotionalAggregate;
import br.jeanjacintho.tideflow.ai_service.repository.CompanyEmotionalAggregateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class StressTimelineService {

    private static final Logger logger = LoggerFactory.getLogger(StressTimelineService.class);

    private static final double STRESS_PEAK_THRESHOLD = 0.30;
    private static final double STRESS_ALERT_THRESHOLD = 0.20;

    private final CompanyEmotionalAggregateRepository companyAggregateRepository;

    public StressTimelineService(CompanyEmotionalAggregateRepository companyAggregateRepository) {
        this.companyAggregateRepository = companyAggregateRepository;
    }

    @Transactional(readOnly = true)
    public StressTimelineDTO getStressTimeline(UUID companyId, LocalDate startDate, LocalDate endDate, String granularity) {
        logger.info("Obtendo timeline de stress para empresa {} de {} a {} com granularidade {}",
            companyId, startDate, endDate, granularity);

        List<CompanyEmotionalAggregate> aggregates = companyAggregateRepository.findByCompanyIdAndDateBetween(
            companyId, startDate, endDate
        );

        if (aggregates.isEmpty()) {
            logger.warn("Nenhuma agregação encontrada para empresa {} no período especificado", companyId);
            return new StressTimelineDTO(
                companyId,
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59),
                granularity,
                new ArrayList<>(),
                new ArrayList<>()
            );
        }

        List<StressTimelineDTO.StressTimelinePoint> points = buildTimelinePoints(aggregates, granularity);
        List<StressTimelineDTO.StressAlert> alerts = detectStressAlerts(points);

        return new StressTimelineDTO(
            companyId,
            startDate.atStartOfDay(),
            endDate.atTime(23, 59, 59),
            granularity,
            points,
            alerts
        );
    }

    private List<StressTimelineDTO.StressTimelinePoint> buildTimelinePoints(
            List<CompanyEmotionalAggregate> aggregates, String granularity) {

        List<StressTimelineDTO.StressTimelinePoint> points = new ArrayList<>();

        for (CompanyEmotionalAggregate aggregate : aggregates) {
            LocalDateTime timestamp = aggregate.getDate().atStartOfDay();

            points.add(new StressTimelineDTO.StressTimelinePoint(
                timestamp,
                aggregate.getAvgStressLevel(),
                aggregate.getTotalActiveUsers(),
                aggregate.getTotalConversations()
            ));
        }

        return points;
    }

    private List<StressTimelineDTO.StressAlert> detectStressAlerts(
            List<StressTimelineDTO.StressTimelinePoint> points) {

        List<StressTimelineDTO.StressAlert> alerts = new ArrayList<>();

        if (points.size() < 2) {
            return alerts;
        }

        for (int i = 1; i < points.size(); i++) {
            StressTimelineDTO.StressTimelinePoint current = points.get(i);
            StressTimelineDTO.StressTimelinePoint previous = points.get(i - 1);

            if (previous.stressLevel() != null && current.stressLevel() != null) {
                double change = (current.stressLevel() - previous.stressLevel()) / previous.stressLevel();

                if (change >= STRESS_PEAK_THRESHOLD) {
                    alerts.add(new StressTimelineDTO.StressAlert(
                        current.timestamp(),
                        "PEAK",
                        String.format("Pico de stress detectado: aumento de %.1f%% em relação ao período anterior",
                            change * 100),
                        current.stressLevel(),
                        change * 100
                    ));
                } else if (change >= STRESS_ALERT_THRESHOLD) {
                    alerts.add(new StressTimelineDTO.StressAlert(
                        current.timestamp(),
                        "ALERT",
                        String.format("Alerta de stress: aumento de %.1f%% em relação ao período anterior",
                            change * 100),
                        current.stressLevel(),
                        change * 100
                    ));
                }
            }
        }

        return alerts;
    }
}
