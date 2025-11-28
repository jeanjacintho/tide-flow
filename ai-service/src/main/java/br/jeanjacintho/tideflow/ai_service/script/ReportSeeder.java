package br.jeanjacintho.tideflow.ai_service.script;

import br.jeanjacintho.tideflow.ai_service.model.CorporateReport;
import br.jeanjacintho.tideflow.ai_service.model.ReportSection;
import br.jeanjacintho.tideflow.ai_service.repository.CorporateReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class ReportSeeder implements CommandLineRunner {

    @Autowired
    private CorporateReportRepository reportRepository;

    private UUID moredevsCompanyId;

    @Override
    @Transactional
    public void run(String... args) {
        if (args.length > 0 && "seed".equals(args[0])) {
            System.out.println("📊 Iniciando população de relatórios...");
            
            seedReports();
            
            System.out.println("✅ Relatórios populados com sucesso!");
        }
    }

    private void seedReports() {
        UUID companyId = getMoredevsCompanyId();
        
        if (companyId == null) {
            System.out.println("⚠️  Empresa 'moredevs' não encontrada. Execute primeiro o seed do user-service.");
            return;
        }

        org.springframework.data.domain.Page<CorporateReport> existingPage = reportRepository.findByCompanyIdOrderByCreatedAtDesc(
            companyId, org.springframework.data.domain.PageRequest.of(0, 1));
        if (existingPage.getTotalElements() > 0) {
            System.out.println("⚠️  Relatórios já existem para a empresa. Pulando criação...");
            return;
        }

        LocalDate today = LocalDate.now();
        List<CorporateReport> reports = new ArrayList<>();

        reports.add(createReport(
            companyId,
            null,
            CorporateReport.ReportType.COMPREHENSIVE,
            "Relatório Mensal Completo - " + today.getMonth().name() + " " + today.getYear(),
            today.minusMonths(1).withDayOfMonth(1),
            today.minusMonths(1).withDayOfMonth(today.minusMonths(1).lengthOfMonth()),
            today.minusMonths(1).withDayOfMonth(15)
        ));

        reports.add(createReport(
            companyId,
            null,
            CorporateReport.ReportType.STRESS_TIMELINE,
            "Análise de Timeline de Estresse - Últimos 30 dias",
            today.minusDays(30),
            today,
            today
        ));

        reports.add(createReport(
            companyId,
            null,
            CorporateReport.ReportType.DEPARTMENT_HEATMAP,
            "Mapa de Calor por Departamento - " + today.getMonth().name(),
            today.withDayOfMonth(1),
            today,
            today
        ));

        reports.add(createReport(
            companyId,
            null,
            CorporateReport.ReportType.TURNOVER_PREDICTION,
            "Predição de Rotatividade - Análise de Riscos",
            today.minusMonths(3).withDayOfMonth(1),
            today,
            today
        ));

        reports.add(createReport(
            companyId,
            null,
            CorporateReport.ReportType.IMPACT_ANALYSIS,
            "Análise de Impacto - Mudanças Organizacionais",
            today.minusDays(60),
            today,
            today.minusDays(30)
        ));

        for (CorporateReport report : reports) {
            reportRepository.save(report);
        }

        System.out.println("✅ " + reports.size() + " relatórios criados para a empresa moredevs");
    }

    private UUID getMoredevsCompanyId() {
        if (moredevsCompanyId != null) {
            return moredevsCompanyId;
        }

        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:8080/api/companies"))
                .header("Content-Type", "application/json")
                .GET()
                .build();
            
            java.net.http.HttpResponse<String> response = client.send(request, 
                java.net.http.HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode companies = mapper.readTree(response.body());
                
                for (com.fasterxml.jackson.databind.JsonNode company : companies) {
                    if ("moredevs".equalsIgnoreCase(company.get("name").asText())) {
                        moredevsCompanyId = UUID.fromString(company.get("id").asText());
                        return moredevsCompanyId;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️  Erro ao buscar companyId via API. Tentando buscar diretamente do banco...");
        }

        return null;
    }

    private CorporateReport createReport(
            UUID companyId,
            UUID departmentId,
            CorporateReport.ReportType reportType,
            String title,
            LocalDate periodStart,
            LocalDate periodEnd,
            LocalDate reportDate) {
        
        CorporateReport report = new CorporateReport();
        report.setCompanyId(companyId);
        report.setDepartmentId(departmentId);
        report.setReportType(reportType);
        report.setTitle(title);
        report.setPeriodStart(periodStart);
        report.setPeriodEnd(periodEnd);
        report.setReportDate(reportDate);
        report.setStatus(CorporateReport.ReportStatus.COMPLETED);
        report.setGeneratedByAi(true);
        report.setAiModelVersion("grok-4.1-fast");
        report.setGenerationTimeMs(1500L + (long)(Math.random() * 3000));
        report.setGeneratedAt(LocalDateTime.now().minusDays((int)(Math.random() * 7)));

        Map<String, Object> insights = new HashMap<>();
        insights.put("averageStressLevel", 6.2 + Math.random() * 2.0);
        insights.put("trend", "estável");
        insights.put("riskLevel", "moderado");
        insights.put("recommendations", Arrays.asList(
            "Implementar programas de bem-estar",
            "Melhorar comunicação interna",
            "Revisar carga de trabalho"
        ));
        report.setInsights(insights);

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalConversations", 150 + (int)(Math.random() * 100));
        metrics.put("activeUsers", 12 + (int)(Math.random() * 8));
        metrics.put("averageResponseTime", 2.5 + Math.random() * 1.5);
        report.setMetrics(metrics);

        report.setExecutiveSummary(
            "Este relatório apresenta uma análise abrangente do bem-estar organizacional da empresa. " +
            "Os dados indicam níveis de estresse moderados, com tendência estável. " +
            "Recomendamos atenção especial aos departamentos com maior carga de trabalho."
        );

        report.setRecommendations(
            "1. Implementar programas de mindfulness e bem-estar\n" +
            "2. Realizar reuniões regulares de feedback\n" +
            "3. Revisar e otimizar processos de trabalho\n" +
            "4. Promover atividades de integração entre equipes"
        );

        List<ReportSection> sections = new ArrayList<>();
        
        ReportSection section1 = new ReportSection();
        section1.setReport(report);
        section1.setSectionType("EXECUTIVE_SUMMARY");
        section1.setSectionOrder(1);
        section1.setTitle("Resumo Executivo");
        section1.setContent(report.getExecutiveSummary());
        sections.add(section1);

        ReportSection section2 = new ReportSection();
        section2.setReport(report);
        section2.setSectionType("METRICS");
        section2.setSectionOrder(2);
        section2.setTitle("Métricas Principais");
        section2.setContent("Total de conversas: " + metrics.get("totalConversations") + 
                           "\nUsuários ativos: " + metrics.get("activeUsers"));
        sections.add(section2);

        ReportSection section3 = new ReportSection();
        section3.setReport(report);
        section3.setSectionType("RECOMMENDATIONS");
        section3.setSectionOrder(3);
        section3.setTitle("Recomendações");
        section3.setContent(report.getRecommendations());
        sections.add(section3);

        report.setSections(sections);

        return report;
    }
}
