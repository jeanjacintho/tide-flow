package br.jeanjacintho.tideflow.ai_service.controller;

import br.jeanjacintho.tideflow.ai_service.dto.request.ConversationRequest;
import br.jeanjacintho.tideflow.ai_service.dto.response.ConversationHistoryResponse;
import br.jeanjacintho.tideflow.ai_service.dto.response.ConversationResponse;
import br.jeanjacintho.tideflow.ai_service.dto.response.ConversationSummaryResponse;
import br.jeanjacintho.tideflow.ai_service.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@CrossOrigin(origins = "*")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    public Mono<ResponseEntity<ConversationResponse>> createConversation(
            @Valid @RequestBody ConversationRequest request) {
        return conversationService.processConversation(request)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.badRequest().build());
    }

    @GetMapping("/{conversationId}")
    public Mono<ResponseEntity<ConversationHistoryResponse>> getConversationHistory(
            @PathVariable String conversationId,
            @RequestHeader("X-User-Id") String userId) {

        return conversationService.getConversationHistory(conversationId, userId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public Mono<ResponseEntity<List<ConversationSummaryResponse>>> getUserConversations(
            @PathVariable String userId) {

        return conversationService.getUserConversations(userId)
                .collectList()
                .map(ResponseEntity::ok);
    }
}


