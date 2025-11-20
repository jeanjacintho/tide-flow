package br.jeanjacintho.tideflow.ai_service.controller;

import br.jeanjacintho.tideflow.ai_service.dto.response.PatternResponse;
import br.jeanjacintho.tideflow.ai_service.model.EmotionalPattern;
import br.jeanjacintho.tideflow.ai_service.service.PatternAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patterns")
@CrossOrigin(origins = "*")
public class PatternController {

    private final PatternAnalysisService patternAnalysisService;

    public PatternController(PatternAnalysisService patternAnalysisService) {
        this.patternAnalysisService = patternAnalysisService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<PatternResponse>> getPatterns(@PathVariable String userId) {
        List<EmotionalPattern> patterns = patternAnalysisService.getPadroes(userId);
        List<PatternResponse> responses = patterns.stream()
                .map(PatternResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/analyze/{userId}")
    public ResponseEntity<Void> triggerAnalysis(@PathVariable String userId) {
        patternAnalysisService.analisarPadroesTemporais(userId);
        return ResponseEntity.accepted().build();
    }
}

