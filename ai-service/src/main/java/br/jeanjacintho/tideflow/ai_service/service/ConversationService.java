package br.jeanjacintho.tideflow.ai_service.service;

import br.jeanjacintho.tideflow.ai_service.client.LLMClient;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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

    private final LLMClient llmClient;
    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final MemoriaService memoriaService;
    private final ObjectMapper objectMapper;
    private final EmotionalAnalysisRepository emotionalAnalysisRepository;
    private final RiskDetectionService riskDetectionService;
    private final RiskAlertPublisher riskAlertPublisher;

    public ConversationService(LLMClient llmClient,
                               ConversationRepository conversationRepository,
                               ConversationMessageRepository conversationMessageRepository,
                               MemoriaService memoriaService,
                               ObjectMapper objectMapper,
                               EmotionalAnalysisRepository emotionalAnalysisRepository,
                               RiskDetectionService riskDetectionService,
                               RiskAlertPublisher riskAlertPublisher) {
        this.llmClient = llmClient;
        this.conversationRepository = conversationRepository;
        this.conversationMessageRepository = conversationMessageRepository;
        this.memoriaService = memoriaService;
        this.objectMapper = objectMapper;
        this.emotionalAnalysisRepository = emotionalAnalysisRepository;
        this.riskDetectionService = riskDetectionService;
        this.riskAlertPublisher = riskAlertPublisher;
    }

    @Transactional
    public Mono<ConversationResponse> processConversation(ConversationRequest request) {
        Conversation conversation = getOrCreateConversation(request.getConversationId(), request.getUserId());

        List<ConversationMessage> existingMessages = conversationMessageRepository.findByConversationIdOrderBySequenceNumberAsc(conversation.getId());

        int nextSequence = existingMessages.size() + 1;
        ConversationMessage userMessage = new ConversationMessage(MessageRole.USER, request.getMessage(), nextSequence);
        conversation.addMessage(userMessage);
        conversationMessageRepository.save(userMessage);

        // Analisa risco de forma assíncrona
        riskDetectionService.analyzeRisk(request.getMessage(), request.getUserId())
                .subscribe(
                    riskAnalysis -> {
                        logger.info("Análise de risco concluída para usuário {}: detectado={}, nível={}, confiança={}", 
                            request.getUserId(), riskAnalysis.isRiskDetected(), riskAnalysis.getRiskLevel(), riskAnalysis.getConfidence());
                        
                        if (riskAnalysis.isRiskDetected()) {
                            logger.info("Risco detectado! Nível: {}, Motivo: {}", riskAnalysis.getRiskLevel(), riskAnalysis.getReason());
                            
                            // Publica alerta para HIGH, CRITICAL ou MEDIUM (adicionando MEDIUM para não perder alertas importantes)
                            if (riskAnalysis.getRiskLevel().equals("HIGH") || 
                                riskAnalysis.getRiskLevel().equals("CRITICAL") ||
                                riskAnalysis.getRiskLevel().equals("MEDIUM")) {
                                logger.info("Publicando alerta de risco para usuário {} com nível {}", request.getUserId(), riskAnalysis.getRiskLevel());
                                riskAlertPublisher.publishRiskAlert(request.getUserId(), request.getMessage(), riskAnalysis);
                            } else {
                                logger.info("Risco detectado mas nível {} não requer alerta imediato", riskAnalysis.getRiskLevel());
                            }
                        } else {
                            logger.debug("Nenhum risco detectado na mensagem do usuário {}", request.getUserId());
                        }
                    }, 
                    error -> {
                        logger.error("Erro ao analisar risco para usuário {}: {}", request.getUserId(), error.getMessage(), error);
                    }
                );

        List<Map<String, String>> history = buildHistoryFromMessages(existingMessages);
        history.add(Map.of("role", "user", "content", request.getMessage()));

        // Recupera memórias relevantes do usuário de forma assíncrona
        Mono<String> memoriasMono = memoriaService.recuperarMemoriasRelevantesAsync(
                request.getUserId(), 
                request.getMessage()
        );

        return memoriasMono.flatMap(memoriasFormatadas -> {
            String systemPrompt = buildSystemPromptWithMemories(memoriasFormatadas);

            List<Map<String, String>> messagesForOllama = new ArrayList<>();
            messagesForOllama.add(Map.of("role", "system", "content", systemPrompt));
            messagesForOllama.addAll(history);

            return llmClient.chatWithHistory(messagesForOllama)
                .flatMap(aiResponse -> {
                    ConversationMessage assistantMessage = new ConversationMessage(
                            MessageRole.ASSISTANT,
                            aiResponse,
                            nextSequence + 1
                    );
                    conversation.addMessage(assistantMessage);
                    conversationMessageRepository.save(assistantMessage);

                    conversationRepository.save(conversation);

                    // Extrai análise emocional e memórias em uma única requisição (otimização)
                    return llmClient.extractEmotionalAnalysisAndMemories(request.getMessage(), aiResponse)
                            .map(jsonResponse -> {
                                try {
                                    // Limpa a resposta se vier com markdown
                                    jsonResponse = jsonResponse.replace("```json", "").replace("```", "").trim();
                                    
                                    Map<String, Object> responseMap;
                                    try {
                                        responseMap = objectMapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {});
                                    } catch (JsonProcessingException e) {
                                        logger.error("Erro ao fazer parse do JSON consolidado: {}", e.getMessage(), e);
                                        throw new RuntimeException("Erro ao processar resposta da IA", e);
                                    }
                                    
                                    // Processa análise emocional
                                    Map<String, Object> analiseEmocionalData = extractMapFromResponse(responseMap, "analiseEmocional");
                                    
                                    EmotionalAnalysis analysis = parseEmotionalAnalysis(analiseEmocionalData);
                                    analysis.setUsuarioId(request.getUserId());
                                    analysis.setConversationId(conversation.getId());
                                    analysis.setMessageId(userMessage.getId());
                                    analysis.setSequenceNumber(userMessage.getSequenceNumber());
                                    emotionalAnalysisRepository.save(analysis);

                                    // Processa memórias e gatilhos de forma assíncrona
                                    memoriaService.processarMensagemParaMemoriaConsolidada(
                                            request.getUserId(),
                                            request.getMessage(),
                                            aiResponse,
                                            responseMap
                                    );

                                    return new ConversationResponse(
                                            aiResponse,
                                            conversation.getId().toString(),
                                            false,
                                            analysis
                                    );
                                } catch (Exception e) {
                                    logger.error("Erro ao processar resposta consolidada: {}", e.getMessage(), e);
                                    EmotionalAnalysis defaultAnalysis = createDefaultEmotionalAnalysis();
                                    defaultAnalysis.setUsuarioId(request.getUserId());
                                    defaultAnalysis.setConversationId(conversation.getId());
                                    defaultAnalysis.setMessageId(userMessage.getId());
                                    defaultAnalysis.setSequenceNumber(userMessage.getSequenceNumber());
                                    emotionalAnalysisRepository.save(defaultAnalysis);
                                    
                                    return new ConversationResponse(
                                            aiResponse,
                                            conversation.getId().toString(),
                                            false,
                                            defaultAnalysis
                                    );
                                }
                            })
                            .onErrorReturn(new ConversationResponse(
                                    aiResponse,
                                    conversation.getId().toString(),
                                    false,
                                    createDefaultEmotionalAnalysis()
                            ));
                });
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
     * Extrai um Map de um responseMap de forma type-safe.
     */
    private Map<String, Object> extractMapFromResponse(Map<String, Object> responseMap, String key) {
        Object obj = responseMap.getOrDefault(key, new HashMap<>());
        if (obj instanceof Map<?, ?>) {
            Map<?, ?> rawMap = (Map<?, ?>) obj;
            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() instanceof String) {
                    result.put((String) entry.getKey(), entry.getValue());
                }
            }
            return result;
        }
        return new HashMap<>();
    }

    /**
     * Extrai uma lista de strings de um Map de forma type-safe.
     */
    private List<String> extractStringListFromMap(Map<String, Object> map, String key) {
        Object obj = map.getOrDefault(key, new ArrayList<>());
        if (obj instanceof List<?>) {
            List<?> rawList = (List<?>) obj;
            List<String> result = new ArrayList<>();
            for (Object item : rawList) {
                if (item instanceof String) {
                    result.add((String) item);
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    /**
     * Parse análise emocional de um Map de dados.
     */
    private EmotionalAnalysis parseEmotionalAnalysis(Map<String, Object> analysisData) {
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
        
        List<String> triggers = extractStringListFromMap(analysisData, "triggers");
        
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
        List<Conversation> conversations = conversationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        // Busca todas as contagens e últimas mensagens de uma vez (evita N+1)
        Map<UUID, Long> messageCounts = new HashMap<>();
        Map<UUID, String> lastMessages = new HashMap<>();
        
        for (Conversation conv : conversations) {
            messageCounts.put(conv.getId(), conversationMessageRepository.countByConversationId(conv.getId()));
            
            conversationMessageRepository.findLastMessageByConversationId(conv.getId())
                    .stream()
                    .findFirst()
                    .ifPresent(msg -> {
                        String content = msg.getContent();
                        lastMessages.put(conv.getId(), 
                                content.length() > 100 ? content.substring(0, 100) + "..." : content);
                    });
        }
        
        return Flux.fromIterable(conversations)
                .map(conversation -> new ConversationSummaryResponse(
                        conversation.getId(),
                        conversation.getUserId(),
                        conversation.getCreatedAt(),
                        conversation.getUpdatedAt(),
                        messageCounts.getOrDefault(conversation.getId(), 0L),
                        lastMessages.getOrDefault(conversation.getId(), "")
                ));
    }
}

