package br.jeanjacintho.tideflow.ai_service.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.jeanjacintho.tideflow.ai_service.client.LLMClient;
import br.jeanjacintho.tideflow.ai_service.model.Memoria;
import br.jeanjacintho.tideflow.ai_service.model.TipoMemoria;
import br.jeanjacintho.tideflow.ai_service.repository.MemoriaRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemoriaService Tests")
@SuppressWarnings("unchecked")
class MemoriaServiceTest {

    @Mock
    private MemoriaRepository memoriaRepository;

    @Mock
    private LLMClient llmClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private TriggerService triggerService;

    @InjectMocks
    private MemoriaService memoriaService;

    private String userId;
    private String userMessage;
    private String aiResponse;

    @BeforeEach
    void setUp() {
        userId = "user-123";
        userMessage = "Estou me sentindo ansioso hoje";
        aiResponse = "Entendo sua ansiedade. Vamos conversar sobre isso.";
    }

    @Test
    @DisplayName("recuperarMemoriasRelevantesAsync - Deve retornar memórias formatadas quando existem")
    void testRecuperarMemoriasRelevantesAsyncWithMemories() {
        List<Memoria> memorias = new ArrayList<>();
        Memoria memoria = new Memoria(userId, "Usuário gosta de café", TipoMemoria.PREFERENCIA, "contexto", 80);
        memorias.add(memoria);

        when(memoriaRepository.findMemoriasRelevantes(userId)).thenReturn(memorias);
        when(memoriaRepository.save(any(Memoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(memoriaService.recuperarMemoriasRelevantesAsync(userId, userMessage))
                .assertNext(formatted -> {
                    assertNotNull(formatted);
                    assertFalse(formatted.isEmpty());
                    assertTrue(formatted.contains("MEMÓRIAS DO USUÁRIO"));
                })
                .verifyComplete();

        verify(memoriaRepository).findMemoriasRelevantes(userId);
    }

    @Test
    @DisplayName("recuperarMemoriasRelevantesAsync - Deve retornar string vazia quando não há memórias")
    void testRecuperarMemoriasRelevantesAsyncEmpty() {
        when(memoriaRepository.findMemoriasRelevantes(userId)).thenReturn(new ArrayList<>());

        StepVerifier.create(memoriaService.recuperarMemoriasRelevantesAsync(userId, userMessage))
                .assertNext(formatted -> {
                    assertTrue(formatted.isEmpty());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("recuperarMemoriasRelevantes - Deve retornar memórias formatadas quando existem")
    void testRecuperarMemoriasRelevantesWithMemories() {
        List<Memoria> memorias = new ArrayList<>();
        Memoria memoria = new Memoria(userId, "Usuário trabalha como desenvolvedor", TipoMemoria.FATO_PESSOAL, "contexto", 70);
        memorias.add(memoria);

        when(memoriaRepository.findMemoriasRelevantes(userId)).thenReturn(memorias);
        when(memoriaRepository.save(any(Memoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String result = memoriaService.recuperarMemoriasRelevantes(userId, userMessage);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("MEMÓRIAS DO USUÁRIO"));
        verify(memoriaRepository).findMemoriasRelevantes(userId);
    }

    @Test
    @DisplayName("processarMensagemParaMemoria - Deve processar e salvar memórias quando extraídas")
    void testProcessarMensagemParaMemoriaSuccess() throws Exception {
        String jsonResponse = "{\"memorias\":[{\"tipo\":\"FATO_PESSOAL\",\"conteudo\":\"Usuário está ansioso\",\"relevancia\":80}]}";
        
        when(llmClient.extractMemories(userMessage, aiResponse)).thenReturn(Mono.just(jsonResponse));
        
        Map<String, Object> responseMap = new HashMap<>();
        List<Map<String, Object>> memoriasData = new ArrayList<>();
        Map<String, Object> memoriaData = new HashMap<>();
        memoriaData.put("tipo", "FATO_PESSOAL");
        memoriaData.put("conteudo", "Usuário está ansioso");
        memoriaData.put("relevancia", 80);
        memoriasData.add(memoriaData);
        responseMap.put("memorias", memoriasData);
        
        java.util.Map<String, Object> responseMapTyped = (java.util.Map<String, Object>) responseMap;
        when(objectMapper.readValue(anyString(), any(Class.class))).thenReturn(responseMapTyped);
        when(memoriaRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        CompletableFuture<Void> future = memoriaService.processarMensagemParaMemoria(userId, userMessage, aiResponse);

        future.join();

        verify(llmClient).extractMemories(userMessage, aiResponse);
        verify(memoriaRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("processarMensagemParaMemoria - Deve processar gatilhos quando presentes")
    void testProcessarMensagemParaMemoriaWithTriggers() throws Exception {
        String jsonResponse = "{\"memorias\":[{\"tipo\":\"FATO_PESSOAL\",\"conteudo\":\"Teste\",\"relevancia\":50}],\"gatilhos\":[{\"tipo\":\"SITUACAO\",\"descricao\":\"Trabalho\"}]}";
        
        when(llmClient.extractMemories(userMessage, aiResponse)).thenReturn(Mono.just(jsonResponse));
        
        Map<String, Object> responseMap = new HashMap<>();
        List<Map<String, Object>> memoriasData = new ArrayList<>();
        Map<String, Object> memoriaData = new HashMap<>();
        memoriaData.put("tipo", "FATO_PESSOAL");
        memoriaData.put("conteudo", "Teste");
        memoriaData.put("relevancia", 50);
        memoriasData.add(memoriaData);
        responseMap.put("memorias", memoriasData);
        responseMap.put("gatilhos", new ArrayList<>());
        
        when(objectMapper.readValue(anyString(), any(Class.class))).thenReturn((java.util.Map<String, Object>) responseMap);
        when(memoriaRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        CompletableFuture<Void> future = memoriaService.processarMensagemParaMemoria(userId, userMessage, aiResponse);

        future.join();

        verify(triggerService).processarGatilhos(userId, responseMap);
    }

    @Test
    @DisplayName("processarMensagemParaMemoria - Deve tratar erro graciosamente")
    void testProcessarMensagemParaMemoriaHandlesError() {
        when(llmClient.extractMemories(userMessage, aiResponse))
                .thenReturn(Mono.error(new RuntimeException("LLM error")));

        CompletableFuture<Void> future = memoriaService.processarMensagemParaMemoria(userId, userMessage, aiResponse);

        future.join();
    }

    @Test
    @DisplayName("sugerirPerguntaProativa - Deve retornar pergunta quando há memórias antigas")
    void testSugerirPerguntaProativaSuccess() {
        List<Memoria> memoriasAntigas = new ArrayList<>();
        Memoria memoria = new Memoria(userId, "Usuário mencionou uma prova importante", TipoMemoria.EVENTO, "contexto", 90);
        memoriasAntigas.add(memoria);

        when(memoriaRepository.findMemoriasNaoReferenciadasRecentemente(anyString(), any()))
                .thenReturn(memoriasAntigas);
        when(llmClient.generateProactiveQuestion(anyString(), anyString()))
                .thenReturn(Mono.just("Como foi a prova que você mencionou?"));
        when(memoriaRepository.save(any(Memoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<String> pergunta = memoriaService.sugerirPerguntaProativa(userId);

        assertTrue(pergunta.isPresent());
        assertFalse(pergunta.get().isEmpty());
        verify(memoriaRepository).save(any(Memoria.class));
    }

    @Test
    @DisplayName("sugerirPerguntaProativa - Deve retornar empty quando não há memórias antigas")
    void testSugerirPerguntaProativaEmpty() {
        when(memoriaRepository.findMemoriasNaoReferenciadasRecentemente(anyString(), any()))
                .thenReturn(new ArrayList<>());

        Optional<String> pergunta = memoriaService.sugerirPerguntaProativa(userId);

        assertFalse(pergunta.isPresent());
    }

    @Test
    @DisplayName("invalidarCacheMemorias - Deve invalidar cache corretamente")
    void testInvalidarCacheMemorias() {
        memoriaService.invalidarCacheMemorias(userId);
    }

    @Test
    @DisplayName("atualizarReferencias - Deve incrementar referências das memórias")
    void testAtualizarReferencias() {
        List<Memoria> memorias = new ArrayList<>();
        Memoria memoria = new Memoria(userId, "Teste", TipoMemoria.FATO_PESSOAL, "contexto", 50);
        memorias.add(memoria);

        when(memoriaRepository.save(any(Memoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        memoriaService.atualizarReferencias(memorias);

        verify(memoriaRepository).save(any(Memoria.class));
    }
}

