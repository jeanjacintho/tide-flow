package br.jeanjacintho.tideflow.ai_service.service;

import br.jeanjacintho.tideflow.ai_service.client.OllamaClient;
import br.jeanjacintho.tideflow.ai_service.dto.request.ConversationRequest;
import br.jeanjacintho.tideflow.ai_service.dto.response.ConversationHistoryResponse;
import br.jeanjacintho.tideflow.ai_service.dto.response.ConversationResponse;
import br.jeanjacintho.tideflow.ai_service.dto.response.ConversationSummaryResponse;
import br.jeanjacintho.tideflow.ai_service.model.Conversation;
import br.jeanjacintho.tideflow.ai_service.model.ConversationMessage;
import br.jeanjacintho.tideflow.ai_service.model.EmotionalAnalysis;
import br.jeanjacintho.tideflow.ai_service.model.MessageRole;
import br.jeanjacintho.tideflow.ai_service.repository.ConversationMessageRepository;
import br.jeanjacintho.tideflow.ai_service.repository.ConversationRepository;
import br.jeanjacintho.tideflow.ai_service.repository.EmotionalAnalysisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationService.class);

    private final OllamaClient ollamaClient;
    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final MemoriaService memoriaService;
    private final ObjectMapper objectMapper;
    private final EmotionalAnalysisRepository emotionalAnalysisRepository;

    public ConversationService(OllamaClient ollamaClient,
                               ConversationRepository conversationRepository,
                               ConversationMessageRepository conversationMessageRepository,
                               MemoriaService memoriaService,
                               ObjectMapper objectMapper,
                               EmotionalAnalysisRepository emotionalAnalysisRepository) {
        this.ollamaClient = ollamaClient;
        this.conversationRepository = conversationRepository;
        this.conversationMessageRepository = conversationMessageRepository;
        this.memoriaService = memoriaService;
        this.objectMapper = objectMapper;
        this.emotionalAnalysisRepository = emotionalAnalysisRepository;
    }

    @Transactional
    public Mono<ConversationResponse> processConversation(ConversationRequest request) {
        Conversation conversation = getOrCreateConversation(request.getConversationId(), request.getUserId());

        List<ConversationMessage> existingMessages = conversationMessageRepository.findByConversationIdOrderBySequenceNumberAsc(conversation.getId());

        int nextSequence = existingMessages.size() + 1;
        ConversationMessage userMessage = new ConversationMessage(MessageRole.USER, request.getMessage(), nextSequence);
        conversation.addMessage(userMessage);
        conversationMessageRepository.save(userMessage);

        List<Map<String, String>> history = buildHistoryFromMessages(existingMessages);
        history.add(Map.of("role", "user", "content", request.getMessage()));

        // Recupera memórias relevantes do usuário
        String memoriasFormatadas = memoriaService.recuperarMemoriasRelevantes(
                request.getUserId(), 
                request.getMessage()
        );

        String systemPrompt = buildSystemPromptWithMemories(memoriasFormatadas);

        List<Map<String, String>> messagesForOllama = new ArrayList<>();
        messagesForOllama.add(Map.of("role", "system", "content", systemPrompt));
        messagesForOllama.addAll(history);

        return ollamaClient.chatWithHistory(messagesForOllama)
                .flatMap(aiResponse -> {
                    ConversationMessage assistantMessage = new ConversationMessage(
                            MessageRole.ASSISTANT,
                            aiResponse,
                            nextSequence + 1
                    );
                    conversation.addMessage(assistantMessage);
                    conversationMessageRepository.save(assistantMessage);

                    conversationRepository.save(conversation);

                    // Extrai análise emocional da mensagem do usuário usando IA
                    return extractEmotionalAnalysis(request.getMessage())
                            .map(analysis -> {
                                // Salva análise emocional associada à mensagem do usuário
                                analysis.setUsuarioId(request.getUserId());
                                analysis.setConversationId(conversation.getId());
                                analysis.setMessageId(userMessage.getId());
                                analysis.setSequenceNumber(userMessage.getSequenceNumber());
                                emotionalAnalysisRepository.save(analysis);

                                // Processa extração de memórias de forma assíncrona
                                memoriaService.processarMensagemParaMemoria(
                                        request.getUserId(),
                                        request.getMessage(),
                                        aiResponse
                                );

                                // Analisa padrões temporais de forma assíncrona (após acumular algumas mensagens)
                                // Será executado periodicamente pelo scheduler, mas também pode ser acionado manualmente
                                
                                return new ConversationResponse(
                                        aiResponse,
                                        conversation.getId().toString(),
                                        false,
                                        analysis
                                );
                            })
                            .defaultIfEmpty(new ConversationResponse(
                                    aiResponse,
                                    conversation.getId().toString(),
                                    false,
                                    createDefaultEmotionalAnalysis()
                            ));
                });
    }

    private Conversation getOrCreateConversation(String conversationId, String userId){
        if(conversationId != null && !conversationId.isBlank()) {
            try {
                UUID uuid = UUID.fromString(conversationId);
                return conversationRepository.findByIdAndUserId(uuid, userId)
                        .orElseGet(() -> createNewConversation(userId));
            } catch(IllegalArgumentException e) {
                return createNewConversation(userId);
            }
        }
        return createNewConversation(userId);
    }

    private Conversation createNewConversation(String userId) {
        Conversation conversation = new Conversation(userId);
        return conversationRepository.save(conversation);
    }

    private List<Map<String, String>> buildHistoryFromMessages(List<ConversationMessage> messages) {
        return messages.stream()
                .map(msg -> Map.of(
                        "role", msg.getRole().name().toLowerCase(),
                        "content", msg.getContent()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Constrói o system prompt incluindo as memórias do usuário.
     */
    private String buildSystemPromptWithMemories(String memoriasFormatadas) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append("Você é um diário pessoal com IA. Seja empático, acolhedor e faça perguntas relevantes.\n");
        promptBuilder.append("Use um tom acolhedor, mas profissional. Faça perguntas curtas e reflexivas que instigam o usuário a se aprofundar e falar mais.\n");
        promptBuilder.append("Valide os sentimentos compartilhados e faça conexões com o que já foi mencionado.\n");
        promptBuilder.append("Evite palavras intimistas como 'amor', 'querido', etc.\n");
        promptBuilder.append("Sempre termine suas respostas com uma pergunta curta que convide o usuário a continuar explorando seus sentimentos.\n");
        promptBuilder.append("Mantenha a conversa fluida e natural, como uma sessão de terapia.\n\n");
        
        if (!memoriasFormatadas.isEmpty()) {
            promptBuilder.append(memoriasFormatadas);
            promptBuilder.append("\n");
        }
        
        promptBuilder.append("IMPORTANTE: Você tem acesso às memórias importantes do usuário. ");
        promptBuilder.append("Use essas informações para fazer perguntas relevantes e mostrar que se lembra de eventos, objetivos e preferências mencionados anteriormente. ");
        promptBuilder.append("Por exemplo, se o usuário mencionou que está esperando resultado de uma prova, você pode perguntar sobre isso em conversas futuras quando for pertinente. ");
        promptBuilder.append("Não mencione explicitamente que está consultando memórias, apenas use o contexto de forma natural.");

        return promptBuilder.toString();
    }

    /**
     * Extrai análise emocional da mensagem do usuário usando IA.
     */
    private Mono<EmotionalAnalysis> extractEmotionalAnalysis(String userMessage) {
        return ollamaClient.extractEmotionalAnalysis(userMessage)
                .map(jsonResponse -> {
                    try {
                        // Limpa a resposta se vier com markdown
                        jsonResponse = jsonResponse.replace("```json", "").replace("```", "").trim();
                        
                        @SuppressWarnings("unchecked")
                        Map<String, Object> analysisData = objectMapper.readValue(jsonResponse, Map.class);
                        
                        String primaryEmotional = (String) analysisData.getOrDefault("primaryEmotional", "neutro");
                        if (primaryEmotional == null || primaryEmotional.isEmpty()) {
                            primaryEmotional = "neutro";
                        }
                        
                        Integer intensity = 50; // Default
                        Object intensityObj = analysisData.get("intensity");
                        if (intensityObj instanceof Number) {
                            intensity = ((Number) intensityObj).intValue();
                            // Garante que está entre 0 e 100
                            intensity = Math.max(0, Math.min(100, intensity));
                        }
                        
                        @SuppressWarnings("unchecked")
                        List<String> triggers = (List<String>) analysisData.getOrDefault("triggers", new ArrayList<>());
                        if (triggers == null) {
                            triggers = new ArrayList<>();
                        }
                        
                        String context = (String) analysisData.getOrDefault("context", "");
                        if (context == null) {
                            context = "";
                        }
                        // Limita o tamanho do contexto
                        if (context.length() > 500) {
                            context = context.substring(0, 497) + "...";
                        }
                        
                        String suggestion = (String) analysisData.getOrDefault("suggestion", 
                                "Continue conversando para entender melhor suas emoções.");
                        if (suggestion == null || suggestion.isEmpty()) {
                            suggestion = "Continue conversando para entender melhor suas emoções.";
                        }
                        
                        return new EmotionalAnalysis(
                                primaryEmotional,
                                intensity,
                                triggers,
                                context,
                                suggestion
                        );
                    } catch (Exception e) {
                        logger.error("Erro ao processar análise emocional: {}", e.getMessage(), e);
                        return createDefaultEmotionalAnalysis();
                    }
                })
                .onErrorReturn(createDefaultEmotionalAnalysis());
    }

    /**
     * Cria uma análise emocional padrão em caso de erro.
     */
    private EmotionalAnalysis createDefaultEmotionalAnalysis() {
        return new EmotionalAnalysis(
                "neutro",
                50,
                new ArrayList<>(),
                "",
                "Continue conversando para entender melhor suas emoções."
        );
    }

    @Transactional(readOnly = true)
    public Mono<ConversationHistoryResponse> getConversationHistory(String conversationId, String userId) {
        try {
            UUID uuid = UUID.fromString(conversationId);
            return Mono.fromCallable(() -> {
                Conversation conversation = conversationRepository
                        .findByIdAndUserId(uuid, userId)
                        .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
                
                List<ConversationMessage> messages = conversationMessageRepository
                        .findByConversationIdOrderBySequenceNumberAsc(conversation.getId());
                
                return new ConversationHistoryResponse(
                        conversation.getId(),
                        conversation.getUserId(),
                        conversation.getCreatedAt(),
                        conversation.getUpdatedAt(),
                        messages
                );
            });
        } catch (IllegalArgumentException e) {
            return Mono.error(new IllegalArgumentException("Invalid conversation ID"));
        }
    }

    @Transactional(readOnly = true)
    public Flux<ConversationSummaryResponse> getUserConversations(String userId) {
        return Flux.fromIterable(conversationRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .map(conversation -> {
                    Long messageCount = conversationMessageRepository.countByConversationId(conversation.getId());
                    
                    String lastMessagePreview = conversationMessageRepository
                            .findLastMessageByConversationId(conversation.getId())
                            .stream()
                            .findFirst()
                            .map(msg -> {
                                String content = msg.getContent();
                                return content.length() > 100 
                                        ? content.substring(0, 100) + "..." 
                                        : content;
                            })
                            .orElse("");
                    
                    return new ConversationSummaryResponse(
                            conversation.getId(),
                            conversation.getUserId(),
                            conversation.getCreatedAt(),
                            conversation.getUpdatedAt(),
                            messageCount,
                            lastMessagePreview
                    );
                });
    }
}

