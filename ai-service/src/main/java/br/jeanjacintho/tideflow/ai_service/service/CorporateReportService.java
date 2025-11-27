package br.jeanjacintho.tideflow.ai_service.service;

import br.jeanjacintho.tideflow.ai_service.client.LLMClient;
import br.jeanjacintho.tideflow.ai_service.dto.request.ReportGenerationRequest;
import br.jeanjacintho.tideflow.ai_service.dto.response.CorporateReportResponseDTO;
import br.jeanjacintho.tideflow.ai_service.dto.response.ReportListResponseDTO;
import br.jeanjacintho.tideflow.ai_service.model.CorporateReport;
import br.jeanjacintho.tideflow.ai_service.model.CorporateReport.ReportStatus;
import br.jeanjacintho.tideflow.ai_service.model.CorporateReport.ReportType;
import br.jeanjacintho.tideflow.ai_service.model.ReportSection;
import br.jeanjacintho.tideflow.ai_service.repository.CorporateReportRepository;
import br.jeanjacintho.tideflow.ai_service.repository.ReportSectionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class CorporateReportService {

    private static final Logger logger = LoggerFactory.getLogger(CorporateReportService.class);
    
    private final CorporateReportRepository reportRepository;
    private final ReportSectionRepository sectionRepository;
    private final LLMClient llmClient;
    private final StressTimelineService stressTimelineService;
    private final DepartmentHeatmapService heatmapService;
    private final TurnoverPredictionService turnoverPredictionService;
    private final ImpactAnalysisService impactAnalysisService;
    private final CorporateDashboardService dashboardService;
    private final ObjectMapper objectMapper;

    public CorporateReportService(
            CorporateReportRepository reportRepository,
            ReportSectionRepository sectionRepository,
            LLMClient llmClient,
            StressTimelineService stressTimelineService,
            DepartmentHeatmapService heatmapService,
            TurnoverPredictionService turnoverPredictionService,
            ImpactAnalysisService impactAnalysisService,
            CorporateDashboardService dashboardService,
            ObjectMapper objectMapper) {
        this.reportRepository = reportRepository;
        this.sectionRepository = sectionRepository;
        this.llmClient = llmClient;
        this.stressTimelineService = stressTimelineService;
        this.heatmapService = heatmapService;
        this.turnoverPredictionService = turnoverPredictionService;
        this.impactAnalysisService = impactAnalysisService;
        this.dashboardService = dashboardService;
        this.objectMapper = objectMapper;
    }

    /**
     * Gera um relatório corporativo de forma assíncrona.
     */
    @Async
    @Transactional
    public CompletableFuture<CorporateReport> generateReportAsync(ReportGenerationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return generateReport(request);
            } catch (Exception e) {
                logger.error("Erro ao gerar relatório assíncrono: {}", e.getMessage(), e);
                throw new RuntimeException("Falha na geração do relatório", e);
            }
        });
    }

    /**
     * Gera um relatório corporativo.
     */
    @Transactional
    public CorporateReport generateReport(ReportGenerationRequest request) {
        long startTime = System.currentTimeMillis();
        logger.info("Iniciando geração de relatório tipo {} para empresa {}", 
            request.getReportType(), request.getCompanyId());

        LocalDate reportDate = LocalDate.now();
        LocalDate periodStart = request.getPeriodStart() != null 
            ? request.getPeriodStart() 
            : reportDate.minusDays(30);
        LocalDate periodEnd = request.getPeriodEnd() != null 
            ? request.getPeriodEnd() 
            : reportDate;

        String title = request.getTitle() != null 
            ? request.getTitle() 
            : generateDefaultTitle(request.getReportType(), periodStart, periodEnd);

        CorporateReport report = new CorporateReport(
            request.getCompanyId(),
            request.getReportType(),
            reportDate,
            periodStart,
            periodEnd,
            title
        );
        report.setDepartmentId(request.getDepartmentId());
        report.setStatus(ReportStatus.GENERATING);
        report.setGeneratedByAi(true);
        report = reportRepository.save(report);

        try {
            Map<String, Object> metrics = collectMetrics(report, request);
            Map<String, Object> insights = generateInsights(report, metrics, request);
            String executiveSummary = generateExecutiveSummary(report, metrics, insights, request);
            String recommendations = request.getGenerateRecommendations() 
                ? generateRecommendations(report, metrics, insights, request)
                : null;

            List<ReportSection> sections = generateSections(report, metrics, insights, request);

            report.setMetrics(metrics);
            report.setInsights(insights);
            report.setExecutiveSummary(executiveSummary);
            report.setRecommendations(recommendations);
            report.setSections(sections);
            report.setStatus(ReportStatus.COMPLETED);
            report.setGeneratedAt(LocalDateTime.now());
            report.setGenerationTimeMs(System.currentTimeMillis() - startTime);

            report = reportRepository.save(report);
            logger.info("Relatório {} gerado com sucesso em {}ms", report.getId(), report.getGenerationTimeMs());
            
            return report;
        } catch (Exception e) {
            logger.error("Erro ao gerar relatório {}: {}", report.getId(), e.getMessage(), e);
            report.setStatus(ReportStatus.FAILED);
            reportRepository.save(report);
            throw new RuntimeException("Falha na geração do relatório: " + e.getMessage(), e);
        }
    }

    /**
     * Coleta métricas baseadas no tipo de relatório.
     */
    private Map<String, Object> collectMetrics(CorporateReport report, ReportGenerationRequest request) {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            switch (report.getReportType()) {
                case STRESS_TIMELINE:
                    metrics.put("stressTimeline", stressTimelineService.getStressTimeline(
                        report.getCompanyId(), report.getPeriodStart(), report.getPeriodEnd(), "day"));
                    break;
                case DEPARTMENT_HEATMAP:
                    metrics.put("heatmap", heatmapService.getDepartmentHeatmap(
                        report.getCompanyId(), report.getReportDate()));
                    break;
                case TURNOVER_PREDICTION:
                    metrics.put("turnoverPrediction", turnoverPredictionService.predictTurnoverRisk(
                        report.getCompanyId(), report.getDepartmentId()));
                    break;
                case IMPACT_ANALYSIS:
                    if (request.getEventDate() != null && request.getEventDescription() != null) {
                        metrics.put("impactAnalysis", impactAnalysisService.analyzeDecisionImpact(
                            report.getCompanyId(), request.getEventDate(), request.getEventDescription()));
                    }
                    break;
                case COMPREHENSIVE:
                    metrics.put("dashboard", dashboardService.getDashboardOverview(
                        report.getCompanyId(), report.getReportDate()));
                    metrics.put("stressTimeline", stressTimelineService.getStressTimeline(
                        report.getCompanyId(), report.getPeriodStart(), report.getPeriodEnd(), "day"));
                    metrics.put("heatmap", heatmapService.getDepartmentHeatmap(
                        report.getCompanyId(), report.getReportDate()));
                    metrics.put("turnoverPrediction", turnoverPredictionService.predictTurnoverRisk(
                        report.getCompanyId(), report.getDepartmentId()));
                    break;
                case CUSTOM:
                    // Custom reports can have custom metrics
                    break;
                default:
                    logger.warn("Tipo de relatório não suportado: {}", report.getReportType());
                    break;
            }
        } catch (Exception e) {
            logger.error("Erro ao coletar métricas: {}", e.getMessage(), e);
        }
        
        return metrics;
    }

    /**
     * Gera insights usando IA.
     */
    private Map<String, Object> generateInsights(CorporateReport report, Map<String, Object> metrics, 
                                                 ReportGenerationRequest request) {
        if (!request.getGenerateInsights()) {
            return new HashMap<>();
        }

        try {
            String prompt = buildInsightsPrompt(report, metrics, request);
            String aiResponse = llmClient.generateResponse(prompt).block();
            
            if (aiResponse != null && !aiResponse.isEmpty()) {
                try {
                    return objectMapper.readValue(aiResponse, new TypeReference<Map<String, Object>>() {});
                } catch (Exception e) {
                    logger.warn("Resposta da IA não é JSON válido, usando como texto: {}", e.getMessage());
                    Map<String, Object> insights = new HashMap<>();
                    insights.put("summary", aiResponse);
                    return insights;
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao gerar insights com IA: {}", e.getMessage(), e);
        }
        
        return generateBasicInsights(metrics);
    }

    /**
     * Gera resumo executivo usando IA.
     */
    private String generateExecutiveSummary(CorporateReport report, Map<String, Object> metrics,
                                           Map<String, Object> insights, ReportGenerationRequest request) {
        try {
            String prompt = buildExecutiveSummaryPrompt(report, metrics, insights, request);
            String aiResponse = llmClient.generateResponse(prompt).block();
            
            if (aiResponse != null && !aiResponse.isEmpty()) {
                return aiResponse.trim();
            }
        } catch (Exception e) {
            logger.error("Erro ao gerar resumo executivo com IA: {}", e.getMessage(), e);
        }
        
        return generateBasicExecutiveSummary(report, metrics);
    }

    /**
     * Gera recomendações usando IA.
     */
    private String generateRecommendations(CorporateReport report, Map<String, Object> metrics,
                                          Map<String, Object> insights, ReportGenerationRequest request) {
        try {
            String prompt = buildRecommendationsPrompt(report, metrics, insights, request);
            String aiResponse = llmClient.generateResponse(prompt).block();
            
            if (aiResponse != null && !aiResponse.isEmpty()) {
                return aiResponse.trim();
            }
        } catch (Exception e) {
            logger.error("Erro ao gerar recomendações com IA: {}", e.getMessage(), e);
        }
        
        return generateBasicRecommendations(metrics);
    }

    /**
     * Gera seções do relatório.
     */
    private List<ReportSection> generateSections(CorporateReport report, Map<String, Object> metrics,
                                                Map<String, Object> insights, ReportGenerationRequest request) {
        List<ReportSection> sections = new ArrayList<>();
        int order = 1;

        if (request.getIncludeSections() == null || request.getIncludeSections().contains("overview")) {
            sections.add(createSection(report, "overview", order++, "Visão Geral", 
                generateOverviewContent(metrics), metrics));
        }

        if (request.getIncludeSections() == null || request.getIncludeSections().contains("metrics")) {
            sections.add(createSection(report, "metrics", order++, "Métricas Detalhadas", 
                generateMetricsContent(metrics), metrics));
        }

        if (request.getIncludeSections() == null || request.getIncludeSections().contains("insights")) {
            sections.add(createSection(report, "insights", order++, "Insights e Análises", 
                generateInsightsContent(insights), insights));
        }

        if (request.getIncludeSections() == null || request.getIncludeSections().contains("trends")) {
            sections.add(createSection(report, "trends", order++, "Tendências", 
                generateTrendsContent(metrics), metrics));
        }

        return sections;
    }

    private ReportSection createSection(CorporateReport report, String sectionType, int order,
                                       String title, String content, Map<String, Object> data) {
        ReportSection section = new ReportSection(report, sectionType, order, title);
        section.setContent(content);
        section.setData(data);
        return section;
    }

    private String generateDefaultTitle(ReportType reportType, LocalDate start, LocalDate end) {
        return String.format("Relatório %s - %s a %s", 
            reportType.name(), start, end);
    }

    private String buildInsightsPrompt(CorporateReport report, Map<String, Object> metrics, 
                                      ReportGenerationRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Você é um analista de RH especializado em bem-estar corporativo. ");
        prompt.append("Analise os seguintes dados e gere insights acionáveis em formato JSON.\n\n");
        prompt.append("Tipo de Relatório: ").append(report.getReportType()).append("\n");
        prompt.append("Período: ").append(report.getPeriodStart()).append(" a ").append(report.getPeriodEnd()).append("\n");
        try {
            prompt.append("Dados: ").append(objectMapper.writeValueAsString(metrics)).append("\n\n");
        } catch (JsonProcessingException e) {
            logger.warn("Erro ao serializar JSON, usando toString: {}", e.getMessage());
            prompt.append("Dados: ").append(metrics.toString()).append("\n\n");
        }
        prompt.append("Gere insights estruturados incluindo: principais descobertas, padrões identificados, ");
        prompt.append("áreas de preocupação e oportunidades de melhoria. Retorne apenas JSON válido.");
        return prompt.toString();
    }

    private String buildExecutiveSummaryPrompt(CorporateReport report, Map<String, Object> metrics,
                                               Map<String, Object> insights, ReportGenerationRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Crie um resumo executivo conciso (2-3 parágrafos) para um relatório corporativo de bem-estar.\n\n");
        prompt.append("Tipo: ").append(report.getReportType()).append("\n");
        prompt.append("Período: ").append(report.getPeriodStart()).append(" a ").append(report.getPeriodEnd()).append("\n");
        try {
            prompt.append("Métricas principais: ").append(objectMapper.writeValueAsString(metrics)).append("\n");
            prompt.append("Insights: ").append(objectMapper.writeValueAsString(insights)).append("\n\n");
        } catch (JsonProcessingException e) {
            logger.warn("Erro ao serializar JSON, usando toString: {}", e.getMessage());
            prompt.append("Métricas principais: ").append(metrics.toString()).append("\n");
            prompt.append("Insights: ").append(insights.toString()).append("\n\n");
        }
        prompt.append("O resumo deve ser claro, direto e focado em ações para gestores.");
        return prompt.toString();
    }

    private String buildRecommendationsPrompt(CorporateReport report, Map<String, Object> metrics,
                                             Map<String, Object> insights, ReportGenerationRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Com base nos dados abaixo, gere recomendações práticas e acionáveis para melhorar ");
        prompt.append("o bem-estar corporativo. Liste 5-7 recomendações prioritárias.\n\n");
        try {
            prompt.append("Dados: ").append(objectMapper.writeValueAsString(metrics)).append("\n");
            prompt.append("Insights: ").append(objectMapper.writeValueAsString(insights)).append("\n");
        } catch (JsonProcessingException e) {
            logger.warn("Erro ao serializar JSON, usando toString: {}", e.getMessage());
            prompt.append("Dados: ").append(metrics.toString()).append("\n");
            prompt.append("Insights: ").append(insights.toString()).append("\n");
        }
        return prompt.toString();
    }

    private Map<String, Object> generateBasicInsights(Map<String, Object> metrics) {
        Map<String, Object> insights = new HashMap<>();
        insights.put("note", "Insights básicos gerados automaticamente");
        return insights;
    }

    private String generateBasicExecutiveSummary(CorporateReport report, Map<String, Object> metrics) {
        return String.format("Relatório %s para o período de %s a %s. " +
            "Análise completa dos dados de bem-estar corporativo.",
            report.getReportType(), report.getPeriodStart(), report.getPeriodEnd());
    }

    private String generateBasicRecommendations(Map<String, Object> metrics) {
        return "1. Monitore continuamente os indicadores de bem-estar\n" +
               "2. Implemente ações preventivas baseadas nos dados\n" +
               "3. Mantenha comunicação aberta com as equipes";
    }

    private String generateOverviewContent(Map<String, Object> metrics) {
        return "Visão geral dos dados coletados no período analisado.";
    }

    private String generateMetricsContent(Map<String, Object> metrics) {
        return "Métricas detalhadas e análises quantitativas.";
    }

    private String generateInsightsContent(Map<String, Object> insights) {
        return "Insights e análises qualitativas baseadas nos dados.";
    }

    private String generateTrendsContent(Map<String, Object> metrics) {
        return "Análise de tendências e padrões identificados.";
    }

    /**
     * Busca relatório por ID.
     */
    @Transactional(readOnly = true)
    public Optional<CorporateReportResponseDTO> getReportById(UUID reportId) {
        return reportRepository.findById(reportId)
            .map(this::toResponseDTO);
    }

    /**
     * Lista relatórios com paginação.
     */
    @Transactional(readOnly = true)
    public ReportListResponseDTO listReports(UUID companyId, ReportType reportType, 
                                            ReportStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CorporateReport> reportPage;
        
        if (reportType != null && status != null) {
            reportPage = reportRepository.findByCompanyIdAndReportTypeAndStatusOrderByCreatedAtDesc(
                companyId, reportType, status, pageable);
        } else if (reportType != null) {
            reportPage = reportRepository.findByCompanyIdAndReportTypeOrderByCreatedAtDesc(
                companyId, reportType, pageable);
        } else if (status != null) {
            reportPage = reportRepository.findByCompanyIdAndStatusOrderByCreatedAtDesc(
                companyId, status, pageable);
        } else {
            reportPage = reportRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, pageable);
        }

        List<ReportListResponseDTO.ReportSummaryDTO> summaries = reportPage.getContent().stream()
            .map(this::toSummaryDTO)
            .collect(Collectors.toList());

        return new ReportListResponseDTO(
            summaries,
            reportPage.getTotalElements(),
            reportPage.getTotalPages(),
            reportPage.getNumber(),
            reportPage.getSize()
        );
    }

    private CorporateReportResponseDTO toResponseDTO(CorporateReport report) {
        List<CorporateReportResponseDTO.ReportSectionDTO> sectionDTOs = 
            sectionRepository.findByReportIdOrderBySectionOrderAsc(report.getId()).stream()
                .map(section -> new CorporateReportResponseDTO.ReportSectionDTO(
                    section.getId(),
                    section.getSectionType(),
                    section.getSectionOrder(),
                    section.getTitle(),
                    section.getContent(),
                    section.getData(),
                    section.getVisualizationConfig()
                ))
                .collect(Collectors.toList());

        return new CorporateReportResponseDTO(
            report.getId(),
            report.getCompanyId(),
            report.getDepartmentId(),
            report.getReportType(),
            report.getReportDate(),
            report.getPeriodStart(),
            report.getPeriodEnd(),
            report.getStatus(),
            report.getTitle(),
            report.getExecutiveSummary(),
            report.getInsights(),
            report.getMetrics(),
            report.getRecommendations(),
            report.getGeneratedByAi(),
            report.getAiModelVersion(),
            report.getGenerationTimeMs(),
            sectionDTOs,
            report.getCreatedAt(),
            report.getUpdatedAt(),
            report.getGeneratedAt()
        );
    }

    private ReportListResponseDTO.ReportSummaryDTO toSummaryDTO(CorporateReport report) {
        return new ReportListResponseDTO.ReportSummaryDTO(
            report.getId(),
            report.getCompanyId(),
            report.getDepartmentId(),
            report.getReportType(),
            report.getReportDate(),
            report.getPeriodStart(),
            report.getPeriodEnd(),
            report.getStatus(),
            report.getTitle(),
            report.getExecutiveSummary(),
            report.getGeneratedByAi(),
            report.getCreatedAt(),
            report.getGeneratedAt()
        );
    }

    /**
     * Deleta um relatório.
     */
    @Transactional
    public void deleteReport(UUID reportId) {
        reportRepository.deleteById(reportId);
    }
}
