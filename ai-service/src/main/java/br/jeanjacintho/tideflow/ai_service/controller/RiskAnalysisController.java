package br.jeanjacintho.tideflow.ai_service.controller;

import br.jeanjacintho.tideflow.ai_service.dto.request.RiskAnalysisRequest;
import br.jeanjacintho.tideflow.ai_service.dto.response.RiskAnalysisResponse;
import br.jeanjacintho.tideflow.ai_service.service.RiskDetectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/risk")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class RiskAnalysisController {

    private final RiskDetectionService riskDetectionService;

    public RiskAnalysisController(RiskDetectionService riskDetectionService) {
        this.riskDetectionService = riskDetectionService;
    }

    @PostMapping("/analyze")
    public Mono<ResponseEntity<RiskAnalysisResponse>> analyzeRisk(@RequestBody RiskAnalysisRequest request) {
        return riskDetectionService.analyzeRisk(request.getMessage(), request.getUserId())
                .map(ResponseEntity::ok);
    }
}
