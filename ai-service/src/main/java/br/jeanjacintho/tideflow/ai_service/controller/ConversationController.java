package br.jeanjacintho.tideflow.ai_service.controller;

import br.jeanjacintho.tideflow.ai_service.client.WhisperClient;
import br.jeanjacintho.tideflow.ai_service.dto.request.ConversationRequest;
import br.jeanjacintho.tideflow.ai_service.dto.response.ConversationHistoryResponse;
import br.jeanjacintho.tideflow.ai_service.dto.response.ConversationResponse;
import br.jeanjacintho.tideflow.ai_service.dto.response.ConversationSummaryResponse;
import br.jeanjacintho.tideflow.ai_service.dto.response.TranscriptionResponse;
import br.jeanjacintho.tideflow.ai_service.service.ConversationService;
import br.jeanjacintho.tideflow.ai_service.service.MemoriaService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@RestController
@RequestMapping("/api/conversations")
@CrossOrigin(origins = "*")
public class ConversationController {

    private final ConversationService conversationService;
    private final WhisperClient whisperClient;
    private final MemoriaService memoriaService;

    public ConversationController(ConversationService conversationService,
                                  WhisperClient whisperClient,
                                  MemoriaService memoriaService) {
        this.conversationService = conversationService;
        this.whisperClient = whisperClient;
        this.memoriaService = memoriaService;
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

    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<TranscriptionResponse>> transcribeAudio(
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam(value = "conversationId", required = false) String conversationId,
            @RequestHeader("X-User-Id") String userId) {

        try {
            byte[] audioData = audioFile.getBytes();
            String filename = audioFile.getOriginalFilename() != null
                    ? audioFile.getOriginalFilename()
                    : "audio.webm";

            return whisperClient.transcribeAudio(audioData, filename)
                    .flatMap(transcript -> {

                        if (conversationId != null && !conversationId.isEmpty() && !transcript.isEmpty()) {
                            ConversationRequest request = new ConversationRequest(userId, transcript, conversationId);
                            return conversationService.processConversation(request)
                                    .map(conversationResponse ->
                                        ResponseEntity.ok(new TranscriptionResponse(transcript, conversationResponse))
                                    )
                                    .defaultIfEmpty(ResponseEntity.ok(new TranscriptionResponse(transcript, null)));
                        }
                        return Mono.just(ResponseEntity.ok(new TranscriptionResponse(transcript, null)));
                    })
                    .defaultIfEmpty(ResponseEntity.badRequest().build());

        } catch (IOException e) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(new TranscriptionResponse("Erro ao processar arquivo de áudio.", null)));
        }
    }

    @GetMapping("/proactive-question/{userId}")
    public Mono<ResponseEntity<Map<String, String>>> getProactiveQuestion(
            @PathVariable String userId) {

        return Mono.fromCallable((Callable<ResponseEntity<Map<String, String>>>) () -> {
            java.util.Optional<String> pergunta = memoriaService.sugerirPerguntaProativa(userId);

            if (pergunta.isPresent()) {
                return ResponseEntity.ok(Map.of("question", pergunta.get()));
            } else {
                return ResponseEntity.ok(Map.of("question", ""));
            }
        });
    }
}
