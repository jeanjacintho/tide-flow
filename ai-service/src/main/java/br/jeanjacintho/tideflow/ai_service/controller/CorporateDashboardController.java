package br.jeanjacintho.tideflow.ai_service.controller;

import br.jeanjacintho.tideflow.ai_service.dto.response.DashboardOverviewDTO;
import br.jeanjacintho.tideflow.ai_service.dto.response.DepartmentHeatmapDTO;
import br.jeanjacintho.tideflow.ai_service.dto.response.ImpactAnalysisDTO;
import br.jeanjacintho.tideflow.ai_service.dto.response.StressTimelineDTO;
import br.jeanjacintho.tideflow.ai_service.dto.response.TurnoverPredictionDTO;
import br.jeanjacintho.tideflow.ai_service.service.CorporateDashboardService;
import br.jeanjacintho.tideflow.ai_service.service.DepartmentHeatmapService;
import br.jeanjacintho.tideflow.ai_service.service.ImpactAnalysisService;
import br.jeanjacintho.tideflow.ai_service.service.StressTimelineService;
import br.jeanjacintho.tideflow.ai_service.service.TurnoverPredictionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/corporate")
@CrossOrigin(origins = "*")
public class CorporateDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(CorporateDashboardController.class);

    private final CorporateDashboardService dashboardService;
    private final StressTimelineService stressTimelineService;
    private final DepartmentHeatmapService heatmapService;
    private final TurnoverPredictionService turnoverPredictionService;
    private final ImpactAnalysisService impactAnalysisService;

    public CorporateDashboardController(
            CorporateDashboardService dashboardService,
            StressTimelineService stressTimelineService,
            DepartmentHeatmapService heatmapService,
            TurnoverPredictionService turnoverPredictionService,
            ImpactAnalysisService impactAnalysisService) {
        this.dashboardService = dashboardService;
        this.stressTimelineService = stressTimelineService;
        this.heatmapService = heatmapService;
        this.turnoverPredictionService = turnoverPredictionService;
        this.impactAnalysisService = impactAnalysisService;
    }

    /**
     * GET /api/corporate/dashboard/{companyId}
     * Retorna dados gerais do dashboard para uma empresa.
     */
    @GetMapping("/dashboard/{companyId}")
    public ResponseEntity<DashboardOverviewDTO> getDashboard(
            @PathVariable UUID companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        logger.info("GET /api/corporate/dashboard/{} - date: {}", companyId, date);
        
        try {
            DashboardOverviewDTO overview = dashboardService.getDashboardOverview(companyId, date);
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            logger.error("Erro ao obter dashboard para empresa {}: {}", companyId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/corporate/stress-timeline/{companyId}
     * Retorna sismógrafo de stress (timeline de stress ao longo do tempo).
     */
    @GetMapping("/stress-timeline/{companyId}")
    public ResponseEntity<StressTimelineDTO> getStressTimeline(
            @PathVariable UUID companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "day") String granularity) {
        logger.info("GET /api/corporate/stress-timeline/{} - {} to {} (granularity: {})", 
            companyId, startDate, endDate, granularity);
        
        try {
            StressTimelineDTO timeline = stressTimelineService.getStressTimeline(
                companyId, startDate, endDate, granularity
            );
            return ResponseEntity.ok(timeline);
        } catch (Exception e) {
            logger.error("Erro ao obter timeline de stress para empresa {}: {}", companyId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/corporate/department-heatmap/{companyId}
     * Retorna mapa de calor por departamento.
     */
    @GetMapping("/department-heatmap/{companyId}")
    public ResponseEntity<DepartmentHeatmapDTO> getDepartmentHeatmap(
            @PathVariable UUID companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        
        logger.info("GET /api/corporate/department-heatmap/{} - date: {}", companyId, date);
        
        try {
            DepartmentHeatmapDTO heatmap = heatmapService.getDepartmentHeatmap(companyId, date);
            return ResponseEntity.ok(heatmap);
        } catch (Exception e) {
            logger.error("Erro ao obter heatmap para empresa {}: {}", companyId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/corporate/turnover-prediction/{companyId}
     * Retorna predição de turnover para uma empresa ou departamento.
     */
    @GetMapping("/turnover-prediction/{companyId}")
    public ResponseEntity<TurnoverPredictionDTO> getTurnoverPrediction(
            @PathVariable UUID companyId,
            @RequestParam(required = false) UUID departmentId) {
        logger.info("GET /api/corporate/turnover-prediction/{} - departmentId: {}", companyId, departmentId);
        
        try {
            TurnoverPredictionDTO prediction = turnoverPredictionService.predictTurnoverRisk(
                companyId, departmentId
            );
            return ResponseEntity.ok(prediction);
        } catch (Exception e) {
            logger.error("Erro ao obter predição de turnover para empresa {}: {}", companyId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/corporate/impact-analysis/{companyId}
     * Retorna análise de impacto de uma decisão ou evento.
     */
    @GetMapping("/impact-analysis/{companyId}")
    public ResponseEntity<ImpactAnalysisDTO> getImpactAnalysis(
            @PathVariable UUID companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
            @RequestParam String eventDescription) {
        logger.info("GET /api/corporate/impact-analysis/{} - eventDate: {}, description: {}", 
            companyId, eventDate, eventDescription);
        
        try {
            ImpactAnalysisDTO impact = impactAnalysisService.analyzeDecisionImpact(
                companyId, eventDate, eventDescription
            );
            return ResponseEntity.ok(impact);
        } catch (Exception e) {
            logger.error("Erro ao analisar impacto para empresa {}: {}", companyId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/corporate/department/{departmentId}/insights
     * Retorna insights detalhados por departamento.
     */
    @GetMapping("/department/{departmentId}/insights")
    public ResponseEntity<DepartmentHeatmapDTO.DepartmentHeatmapItem> getDepartmentInsights(
            @PathVariable UUID departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        
        logger.info("GET /api/corporate/department/{}/insights - date: {}", departmentId, date);
        
        try {
            DepartmentHeatmapDTO.DepartmentHeatmapItem item = heatmapService.getDepartmentInsights(departmentId, date);
            
            if (item == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(item);
        } catch (Exception e) {
            logger.error("Erro ao obter insights do departamento {}: {}", departmentId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
