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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private final OllamaClient ollamaClient;
    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final MemoriaService memoriaService;

    public ConversationService(OllamaClient ollamaClient,
                               ConversationRepository conversationRepository,
                               ConversationMessageRepository conversationMessageRepository,
                               MemoriaService memoriaService) {
        this.ollamaClient = ollamaClient;
        this.conversationRepository = conversationRepository;
        this.conversationMessageRepository = conversationMessageRepository;
        this.memoriaService = memoriaService;
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
                .map(aiResponse -> {
                    ConversationMessage assistantMessage = new ConversationMessage(
                            MessageRole.ASSISTANT,
                            aiResponse,
                            nextSequence + 1
                    );
                    conversation.addMessage(assistantMessage);
                    conversationMessageRepository.save(assistantMessage);

                    conversationRepository.save(conversation);

                    EmotionalAnalysis analysis = extractEmotionalAnalysis(aiResponse);

                    // Processa extração de memórias de forma assíncrona
                    memoriaService.processarMensagemParaMemoria(
                            request.getUserId(),
                            request.getMessage(),
                            aiResponse
                    );

                    return new ConversationResponse(
                            aiResponse,
                            conversation.getId().toString(),
                            false,
                            analysis
                    );
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

    private EmotionalAnalysis extractEmotionalAnalysis(String aiResponse) {
        String lowerResponse = aiResponse.toLowerCase();
        
        String primaryEmotional = "neutro";
        if (lowerResponse.contains("triste") || lowerResponse.contains("tristeza")) {
            primaryEmotional = "tristeza";
        } else if (lowerResponse.contains("ansioso") || lowerResponse.contains("ansiedade")) {
            primaryEmotional = "ansiedade";
        } else if (lowerResponse.contains("feliz") || lowerResponse.contains("alegria")) {
            primaryEmotional = "alegria";
        } else if (lowerResponse.contains("raiva") || lowerResponse.contains("irritado")) {
            primaryEmotional = "raiva";
        }

        int intensity = 5;
        if (lowerResponse.contains("muito") || lowerResponse.contains("extremamente")) {
            intensity = 8;
        } else if (lowerResponse.contains("pouco") || lowerResponse.contains("levemente")) {
            intensity = 3;
        }

        List<String> triggers = new ArrayList<>();
        if (lowerResponse.contains("trabalho")) triggers.add("trabalho");
        if (lowerResponse.contains("relacionamento")) triggers.add("relacionamento");
        if (lowerResponse.contains("família")) triggers.add("família");

        return new EmotionalAnalysis(
                primaryEmotional,
                intensity,
                triggers,
                aiResponse.substring(0, Math.min(200, aiResponse.length())),
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

