package br.jeanjacintho.tideflow.ai_service.controller;

import br.jeanjacintho.tideflow.ai_service.config.TokenValidationService;
import br.jeanjacintho.tideflow.ai_service.dto.request.ReportGenerationRequest;
import br.jeanjacintho.tideflow.ai_service.dto.response.CorporateReportResponseDTO;
import br.jeanjacintho.tideflow.ai_service.dto.response.ReportListResponseDTO;
import br.jeanjacintho.tideflow.ai_service.model.CorporateReport;
import br.jeanjacintho.tideflow.ai_service.model.CorporateReport.ReportStatus;
import br.jeanjacintho.tideflow.ai_service.model.CorporateReport.ReportType;
import br.jeanjacintho.tideflow.ai_service.service.CorporateReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/corporate/reports")
@CrossOrigin(origins = "*")
public class CorporateReportController {

    private static final Logger logger = LoggerFactory.getLogger(CorporateReportController.class);

    private final CorporateReportService reportService;
    private final TokenValidationService tokenValidationService;

    public CorporateReportController(
            CorporateReportService reportService,
            TokenValidationService tokenValidationService) {
        this.reportService = reportService;
        this.tokenValidationService = tokenValidationService;
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.replace("Bearer ", "");
        }
        return null;
    }

    private boolean hasPermission(HttpServletRequest request, UUID companyId) {
        String token = getTokenFromRequest(request);
        if (token == null) {
            return false;
        }
        return tokenValidationService.canAccessCorporateReports(token)
            && tokenValidationService.canAccessCompany(token, companyId);
    }

    @PostMapping("/generate")
    public ResponseEntity<CorporateReportResponseDTO> generateReport(
            @Valid @RequestBody ReportGenerationRequest request,
            HttpServletRequest httpRequest) {
        logger.info("POST /api/corporate/reports/generate - companyId: {}, type: {}",
            request.getCompanyId(), request.getReportType());

        if (!hasPermission(httpRequest, request.getCompanyId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            CorporateReport report = reportService.generateReport(request);
            CorporateReportResponseDTO response = reportService.getReportById(report.getId())
                .orElseThrow(() -> new RuntimeException("Relatório não encontrado após criação"));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Erro ao gerar relatório: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/generate-async")
    public ResponseEntity<CorporateReportResponseDTO> generateReportAsync(
            @Valid @RequestBody ReportGenerationRequest request,
            HttpServletRequest httpRequest) {
        logger.info("POST /api/corporate/reports/generate-async - companyId: {}, type: {}",
            request.getCompanyId(), request.getReportType());

        if (!hasPermission(httpRequest, request.getCompanyId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            CompletableFuture<CorporateReport> future = reportService.generateReportAsync(request);
            CorporateReport report = future.get();
            CorporateReportResponseDTO response = reportService.getReportById(report.getId())
                .orElseThrow(() -> new RuntimeException("Relatório não encontrado após criação"));

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (Exception e) {
            logger.error("Erro ao gerar relatório assíncrono: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<CorporateReportResponseDTO> getReport(
            @PathVariable UUID reportId,
            HttpServletRequest request) {
        logger.info("GET /api/corporate/reports/{}", reportId);

        Optional<CorporateReportResponseDTO> reportOpt = reportService.getReportById(reportId);

        if (reportOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CorporateReportResponseDTO report = reportOpt.get();
        if (!hasPermission(request, report.getCompanyId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(report);
    }

    @GetMapping
    public ResponseEntity<ReportListResponseDTO> listReports(
            @RequestParam UUID companyId,
            @RequestParam(required = false) ReportType reportType,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        logger.info("GET /api/corporate/reports - companyId: {}, type: {}, status: {}, page: {}, size: {}",
            companyId, reportType, status, page, size);

        if (!hasPermission(request, companyId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            ReportListResponseDTO response = reportService.listReports(companyId, reportType, status, page, size);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao listar relatórios: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/company/{companyId}/date-range")
    public ResponseEntity<ReportListResponseDTO> getReportsByDateRange(
            @PathVariable UUID companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        logger.info("GET /api/corporate/reports/company/{}/date-range - {} to {}",
            companyId, startDate, endDate);

        if (!hasPermission(request, companyId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            ReportListResponseDTO response = reportService.listReports(companyId, null, null, page, size);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao buscar relatórios por intervalo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{reportId}")
    public ResponseEntity<Void> deleteReport(@PathVariable UUID reportId) {
        logger.info("DELETE /api/corporate/reports/{}", reportId);

        try {
            reportService.deleteReport(reportId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Erro ao deletar relatório: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/company/{companyId}/latest")
    public ResponseEntity<CorporateReportResponseDTO> getLatestReport(
            @PathVariable UUID companyId,
            @RequestParam(required = false) ReportType reportType,
            HttpServletRequest request) {
        logger.info("GET /api/corporate/reports/company/{}/latest - type: {}", companyId, reportType);

        if (!hasPermission(request, companyId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            ReportListResponseDTO response = reportService.listReports(companyId, reportType, ReportStatus.COMPLETED, 0, 1);

            if (response.getReports() != null && !response.getReports().isEmpty()) {
                UUID latestReportId = response.getReports().get(0).getId();
                Optional<CorporateReportResponseDTO> reportOpt = reportService.getReportById(latestReportId);

                if (reportOpt.isPresent()) {
                    return ResponseEntity.ok(reportOpt.get());
                }
            }

            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Erro ao buscar relatório mais recente: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
