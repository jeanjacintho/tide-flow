package br.jeanjacintho.tideflow.ai_service.controller;

import br.jeanjacintho.tideflow.ai_service.dto.response.TriggerResponse;
import br.jeanjacintho.tideflow.ai_service.model.Trigger;
import br.jeanjacintho.tideflow.ai_service.service.TriggerAnalysisService;
import br.jeanjacintho.tideflow.ai_service.service.TriggerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/triggers")
@CrossOrigin(origins = "*")
public class TriggerController {

    private final TriggerService triggerService;
    private final TriggerAnalysisService triggerAnalysisService;

    public TriggerController(TriggerService triggerService, TriggerAnalysisService triggerAnalysisService) {
        this.triggerService = triggerService;
        this.triggerAnalysisService = triggerAnalysisService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<TriggerResponse>> getTriggers(@PathVariable String userId) {
        List<Trigger> triggers = triggerService.getGatilhos(userId);
        List<TriggerResponse> responses = triggers.stream()
                .map(TriggerResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{userId}/positivos")
    public ResponseEntity<List<TriggerResponse>> getTriggersPositivos(@PathVariable String userId) {
        List<Trigger> triggers = triggerService.getGatilhosPositivos(userId);
        List<TriggerResponse> responses = triggers.stream()
                .map(TriggerResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{userId}/negativos")
    public ResponseEntity<List<TriggerResponse>> getTriggersNegativos(@PathVariable String userId) {
        List<Trigger> triggers = triggerService.getGatilhosNegativos(userId);
        List<TriggerResponse> responses = triggers.stream()
                .map(TriggerResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/analyze/{userId}")
    public ResponseEntity<Void> triggerAnalysis(@PathVariable String userId) {
        triggerAnalysisService.analisarCorrelacaoGatilhoEmocao(userId);
        return ResponseEntity.accepted().build();
    }
}

