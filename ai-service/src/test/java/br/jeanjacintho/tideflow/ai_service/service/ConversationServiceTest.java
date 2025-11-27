package br.jeanjacintho.tideflow.ai_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.jeanjacintho.tideflow.ai_service.client.LLMClient;
import br.jeanjacintho.tideflow.ai_service.dto.request.ConversationRequest;
import br.jeanjacintho.tideflow.ai_service.model.Conversation;
import br.jeanjacintho.tideflow.ai_service.dto.response.RiskAnalysisResponse;
import br.jeanjacintho.tideflow.ai_service.model.ConversationMessage;
import br.jeanjacintho.tideflow.ai_service.model.EmotionalAnalysis;
import br.jeanjacintho.tideflow.ai_service.model.MessageRole;
import br.jeanjacintho.tideflow.ai_service.repository.ConversationMessageRepository;
import br.jeanjacintho.tideflow.ai_service.repository.ConversationRepository;
import br.jeanjacintho.tideflow.ai_service.repository.EmotionalAnalysisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConversationService Tests")
@SuppressWarnings("unchecked")
class ConversationServiceTest {

    @Mock
    private LLMClient llmClient;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMessageRepository conversationMessageRepository;

    @Mock
    private MemoriaService memoriaService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private EmotionalAnalysisRepository emotionalAnalysisRepository;

    @Mock
    private RiskDetectionService riskDetectionService;

    @Mock
    private RiskAlertPublisher riskAlertPublisher;

    @Mock
    private EmotionalAggregationService aggregationService;

    @Mock
    private UserInfoService userInfoService;

    @InjectMocks
    private ConversationService conversationService;

    private ConversationRequest request;
    private Conversation conversation;
    private String userId;
    private UUID conversationId;

    @BeforeEach
    void setUp() {
        userId = "user-123";
        conversationId = UUID.randomUUID();
        request = new ConversationRequest(userId, "Olá, como você está?", null);

        conversation = new Conversation(userId);
        conversation.setId(conversationId);
    }

    @Test
    @DisplayName("processConversation - Deve criar nova conversação quando conversationId é null")
    void testProcessConversationCreatesNewConversation() throws Exception {
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation conv = invocation.getArgument(0);
            if (conv.getId() == null) {
                conv.setId(UUID.randomUUID());
            }
            return conv;
        });
        when(conversationMessageRepository.findByConversationIdOrderBySequenceNumberAsc(any(UUID.class)))
                .thenReturn(new ArrayList<>());
        when(conversationMessageRepository.save(any(ConversationMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emotionalAnalysisRepository.save(any(EmotionalAnalysis.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memoriaService.recuperarMemoriasRelevantesAsync(anyString(), anyString()))
                .thenReturn(Mono.just(""));
        when(riskDetectionService.analyzeRisk(anyString(), anyString()))
                .thenReturn(Mono.just(new RiskAnalysisResponse(false, "NONE", "No risk detected", null, 0.0)));
        when(llmClient.chatWithHistory(anyList())).thenReturn(Mono.just("Resposta da IA"));
        when(llmClient.extractEmotionalAnalysisAndMemories(anyString(), anyString()))
                .thenReturn(Mono.just(createConsolidatedResponseJson()));
        doAnswer(invocation -> createConsolidatedResponseMap())
                .when(objectMapper).readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class));
        when(userInfoService.getUserInfo(anyString(), any())).thenReturn(java.util.Optional.empty());

        StepVerifier.create(conversationService.processConversation(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertNotNull(response.getConversationId());
                    assertNotNull(response.getAiResponse());
                })
                .verifyComplete();

        verify(conversationRepository, org.mockito.Mockito.atLeastOnce()).save(any(Conversation.class));
        verify(conversationMessageRepository, org.mockito.Mockito.atLeastOnce()).save(any(ConversationMessage.class));
    }

    @Test
    @DisplayName("processConversation - Deve usar conversação existente quando conversationId é fornecido")
    void testProcessConversationUsesExistingConversation() throws Exception {
        request.setConversationId(conversationId.toString());
        when(conversationRepository.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.of(conversation));
        when(conversationMessageRepository.findByConversationIdOrderBySequenceNumberAsc(conversationId))
                .thenReturn(new ArrayList<>());
        when(conversationMessageRepository.save(any(ConversationMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emotionalAnalysisRepository.save(any(EmotionalAnalysis.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(memoriaService.recuperarMemoriasRelevantesAsync(anyString(), anyString()))
                .thenReturn(Mono.just(""));
        when(riskDetectionService.analyzeRisk(anyString(), anyString()))
                .thenReturn(Mono.just(new RiskAnalysisResponse(false, "NONE", "No risk detected", null, 0.0)));
        when(llmClient.chatWithHistory(anyList())).thenReturn(Mono.just("Resposta da IA"));
        when(llmClient.extractEmotionalAnalysisAndMemories(anyString(), anyString()))
                .thenReturn(Mono.just(createConsolidatedResponseJson()));
        doAnswer(invocation -> createConsolidatedResponseMap())
                .when(objectMapper).readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class));
        when(userInfoService.getUserInfo(anyString(), any())).thenReturn(java.util.Optional.empty());

        StepVerifier.create(conversationService.processConversation(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(conversationId.toString(), response.getConversationId());
                })
                .verifyComplete();

        verify(conversationRepository).findByIdAndUserId(conversationId, userId);
    }

    @Test
    @DisplayName("processConversation - Deve salvar análise emocional quando extraída com sucesso")
    void testProcessConversationSavesEmotionalAnalysis() throws Exception {
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(conversationMessageRepository.findByConversationIdOrderBySequenceNumberAsc(any(UUID.class)))
                .thenReturn(new ArrayList<>());
        when(conversationMessageRepository.save(any(ConversationMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emotionalAnalysisRepository.save(any(EmotionalAnalysis.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memoriaService.recuperarMemoriasRelevantesAsync(anyString(), anyString()))
                .thenReturn(Mono.just(""));
        when(riskDetectionService.analyzeRisk(anyString(), anyString()))
                .thenReturn(Mono.just(new RiskAnalysisResponse(false, "NONE", "No risk detected", null, 0.0)));
        when(llmClient.chatWithHistory(anyList())).thenReturn(Mono.just("Resposta da IA"));
        when(llmClient.extractEmotionalAnalysisAndMemories(anyString(), anyString()))
                .thenReturn(Mono.just(createConsolidatedResponseJson()));
        doAnswer(invocation -> createConsolidatedResponseMap())
                .when(objectMapper).readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class));
        when(userInfoService.getUserInfo(anyString(), any())).thenReturn(java.util.Optional.empty());

        StepVerifier.create(conversationService.processConversation(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertNotNull(response.getAnalisys());
                })
                .verifyComplete();

        verify(emotionalAnalysisRepository, org.mockito.Mockito.atLeastOnce()).save(any(EmotionalAnalysis.class));
    }

    @Test
    @DisplayName("processConversation - Deve usar análise emocional padrão quando extração falha")
    void testProcessConversationUsesDefaultEmotionalAnalysis() throws Exception {
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(conversationMessageRepository.findByConversationIdOrderBySequenceNumberAsc(any(UUID.class)))
                .thenReturn(new ArrayList<>());
        when(conversationMessageRepository.save(any(ConversationMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(emotionalAnalysisRepository.save(any(EmotionalAnalysis.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memoriaService.recuperarMemoriasRelevantesAsync(anyString(), anyString()))
                .thenReturn(Mono.just(""));
        when(riskDetectionService.analyzeRisk(anyString(), anyString()))
                .thenReturn(Mono.just(new RiskAnalysisResponse(false, "NONE", "No risk detected", null, 0.0)));
        when(llmClient.chatWithHistory(anyList())).thenReturn(Mono.just("Resposta da IA"));
        when(llmClient.extractEmotionalAnalysisAndMemories(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Extraction failed")));
        lenient().when(userInfoService.getUserInfo(anyString(), any())).thenReturn(java.util.Optional.empty());

        StepVerifier.create(conversationService.processConversation(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertNotNull(response.getAnalisys());
                    assertEquals("neutro", response.getAnalisys().getPrimaryEmotional());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getConversationHistory - Deve retornar histórico quando conversação existe")
    void testGetConversationHistorySuccess() {
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(conversation));
        
        List<ConversationMessage> messages = new ArrayList<>();
        ConversationMessage msg = new ConversationMessage(MessageRole.USER, "Mensagem teste", 1);
        messages.add(msg);
        
        when(conversationMessageRepository.findByConversationIdOrderBySequenceNumberAsc(conversationId))
                .thenReturn(messages);

        StepVerifier.create(conversationService.getConversationHistory(conversationId.toString(), userId))
                .assertNext(history -> {
                    assertNotNull(history);
                    assertEquals(conversationId, history.getConversationId());
                    assertEquals(1, history.getMessages().size());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getConversationHistory - Deve retornar erro quando conversação não existe")
    void testGetConversationHistoryNotFound() {
        when(conversationRepository.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.empty());

        StepVerifier.create(conversationService.getConversationHistory(conversationId.toString(), userId))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("getUserConversations - Deve retornar lista de conversações")
    void testGetUserConversations() {
        List<Conversation> conversations = new ArrayList<>();
        conversations.add(conversation);
        
        when(conversationRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(conversations);
        when(conversationMessageRepository.countByConversationId(conversationId))
                .thenReturn(5L);
        when(conversationMessageRepository.findLastMessageByConversationId(conversationId))
                .thenReturn(new ArrayList<>());

        StepVerifier.create(conversationService.getUserConversations(userId))
                .assertNext(summary -> {
                    assertNotNull(summary);
                    assertEquals(conversationId, summary.getConversationId());
                    assertEquals(5L, summary.getMessageCount());
                })
                .verifyComplete();
    }

    private java.util.Map<String, Object> createEmotionalAnalysisMap() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("primaryEmotional", "neutro");
        map.put("intensity", 50);
        map.put("triggers", new ArrayList<>());
        map.put("context", "");
        map.put("suggestion", "Continue conversando");
        return map;
    }

    private String createConsolidatedResponseJson() {
        return "{\"analiseEmocional\":{\"primaryEmotional\":\"neutro\",\"intensity\":50,\"triggers\":[],\"context\":\"\",\"suggestion\":\"Continue conversando\"},\"memorias\":[]}";
    }

    private java.util.Map<String, Object> createConsolidatedResponseMap() {
        java.util.Map<String, Object> consolidated = new java.util.HashMap<>();
        consolidated.put("analiseEmocional", createEmotionalAnalysisMap());
        consolidated.put("memorias", new ArrayList<>());
        return consolidated;
    }
}

