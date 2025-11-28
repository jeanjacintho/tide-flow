package br.jeanjacintho.tideflow.ai_service.scheduler;

import br.jeanjacintho.tideflow.ai_service.dto.request.ReportGenerationRequest;
import br.jeanjacintho.tideflow.ai_service.model.CorporateReport.ReportStatus;
import br.jeanjacintho.tideflow.ai_service.model.CorporateReport.ReportType;
import br.jeanjacintho.tideflow.ai_service.repository.CorporateReportRepository;
import br.jeanjacintho.tideflow.ai_service.repository.EmotionalAnalysisRepository;
import br.jeanjacintho.tideflow.ai_service.service.CorporateReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
public class CorporateReportScheduler {

    private static final Logger logger = LoggerFactory.getLogger(CorporateReportScheduler.class);

    private final CorporateReportService reportService;
    private final CorporateReportRepository reportRepository;
    private final EmotionalAnalysisRepository emotionalAnalysisRepository;

    public CorporateReportScheduler(
            CorporateReportService reportService,
            CorporateReportRepository reportRepository,
            EmotionalAnalysisRepository emotionalAnalysisRepository) {
        this.reportService = reportService;
        this.reportRepository = reportRepository;
        this.emotionalAnalysisRepository = emotionalAnalysisRepository;
    }

    @Scheduled(cron = "0 0 6 * * MON")
    public void generateWeeklyReports() {
        logger.info("Iniciando geração automática de relatórios semanais");

        try {
            LocalDate endDate = LocalDate.now().minusDays(1);
            LocalDate startDate = endDate.minusDays(7);

            List<UUID> companyIds = emotionalAnalysisRepository.findDistinctCompanyIdsByDateRange(
                startDate, endDate);

            logger.info("Encontradas {} empresas para gerar relatórios semanais", companyIds.size());

            for (UUID companyId : companyIds) {
                try {
                    generateWeeklyReportForCompany(companyId, startDate, endDate);
                } catch (Exception e) {
                    logger.error("Erro ao gerar relatório semanal para empresa {}: {}",
                        companyId, e.getMessage(), e);
                }
            }

            logger.info("Geração automática de relatórios semanais concluída");
        } catch (Exception e) {
            logger.error("Erro na geração automática de relatórios semanais: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 3 1 * ?")
    public void generateMonthlyReports() {
        logger.info("Iniciando geração automática de relatórios mensais");

        try {
            LocalDate endDate = LocalDate.now().minusDays(1);
            LocalDate startDate = endDate.minusMonths(1).withDayOfMonth(1);

            List<UUID> companyIds = emotionalAnalysisRepository.findDistinctCompanyIdsByDateRange(
                startDate, endDate);

            logger.info("Encontradas {} empresas para gerar relatórios mensais", companyIds.size());

            for (UUID companyId : companyIds) {
                try {
                    generateMonthlyReportForCompany(companyId, startDate, endDate);
                } catch (Exception e) {
                    logger.error("Erro ao gerar relatório mensal para empresa {}: {}",
                        companyId, e.getMessage(), e);
                }
            }

            logger.info("Geração automática de relatórios mensais concluída");
        } catch (Exception e) {
            logger.error("Erro na geração automática de relatórios mensais: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 4 * * SUN")
    public void archiveOldReports() {
        logger.info("Iniciando arquivamento de relatórios antigos");

        try {
            java.time.LocalDateTime oneYearAgo = java.time.LocalDateTime.now().minusYears(1);

            List<br.jeanjacintho.tideflow.ai_service.model.CorporateReport> oldReports =
                reportRepository.findByStatusAndGeneratedAtBefore(
                    ReportStatus.COMPLETED, oneYearAgo);

            logger.info("Encontrados {} relatórios antigos para arquivar", oldReports.size());

            for (br.jeanjacintho.tideflow.ai_service.model.CorporateReport report : oldReports) {
                try {
                    report.setStatus(ReportStatus.ARCHIVED);
                    reportRepository.save(report);
                } catch (Exception e) {
                    logger.error("Erro ao arquivar relatório {}: {}", report.getId(), e.getMessage(), e);
                }
            }

            logger.info("Arquivamento de relatórios antigos concluído");
        } catch (Exception e) {
            logger.error("Erro no arquivamento de relatórios antigos: {}", e.getMessage(), e);
        }
    }

    private void generateWeeklyReportForCompany(UUID companyId, LocalDate startDate, LocalDate endDate) {
        ReportGenerationRequest request = new ReportGenerationRequest();
        request.setCompanyId(companyId);
        request.setReportType(ReportType.COMPREHENSIVE);
        request.setPeriodStart(startDate);
        request.setPeriodEnd(endDate);
        request.setTitle(String.format("Relatório Semanal - %s a %s", startDate, endDate));
        request.setGenerateInsights(true);
        request.setGenerateRecommendations(true);

        reportService.generateReportAsync(request);
        logger.info("Relatório semanal agendado para empresa {}", companyId);
    }

    private void generateMonthlyReportForCompany(UUID companyId, LocalDate startDate, LocalDate endDate) {
        ReportGenerationRequest request = new ReportGenerationRequest();
        request.setCompanyId(companyId);
        request.setReportType(ReportType.COMPREHENSIVE);
        request.setPeriodStart(startDate);
        request.setPeriodEnd(endDate);
        request.setTitle(String.format("Relatório Mensal - %s a %s", startDate, endDate));
        request.setGenerateInsights(true);
        request.setGenerateRecommendations(true);

        reportService.generateReportAsync(request);
        logger.info("Relatório mensal agendado para empresa {}", companyId);
    }
}
