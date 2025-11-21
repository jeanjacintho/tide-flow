package br.jeanjacintho.tideflow.ai_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.jeanjacintho.tideflow.ai_service.model.Trigger;
import br.jeanjacintho.tideflow.ai_service.model.TipoGatilho;
import br.jeanjacintho.tideflow.ai_service.repository.TriggerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@ExtendWith(MockitoExtension.class)
@DisplayName("TriggerService Tests")
class TriggerServiceTest {

    @Mock
    private TriggerRepository triggerRepository;

    @InjectMocks
    private TriggerService triggerService;

    private String userId;
    private Map<String, Object> memoriaData;

    @BeforeEach
    void setUp() {
        userId = "user-123";
        memoriaData = new HashMap<>();
    }

    @Test
    @DisplayName("processarGatilhos - Deve criar novo gatilho quando não existe similar")
    void testProcessarGatilhosCreatesNew() {
        List<Map<String, Object>> gatilhosData = new ArrayList<>();
        Map<String, Object> gatilhoData = new HashMap<>();
        gatilhoData.put("tipo", "SITUACAO");
        gatilhoData.put("descricao", "Trabalho estressante");
        gatilhoData.put("impacto", 8);
        gatilhoData.put("emocaoAssociada", "ansiedade");
        gatilhoData.put("contexto", "Mencionado várias vezes");
        gatilhoData.put("positivo", false);
        gatilhosData.add(gatilhoData);
        memoriaData.put("gatilhos", gatilhosData);

        when(triggerRepository.findByUsuarioIdAndDescricaoSimilar(userId, "Trabalho estressante"))
                .thenReturn(Optional.empty());
        when(triggerRepository.save(any(Trigger.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompletableFuture<Void> future = triggerService.processarGatilhos(userId, memoriaData);

        future.join();

        verify(triggerRepository).save(any(Trigger.class));
    }

    @Test
    @DisplayName("processarGatilhos - Deve atualizar gatilho existente quando similar encontrado")
    void testProcessarGatilhosUpdatesExisting() {
        List<Map<String, Object>> gatilhosData = new ArrayList<>();
        Map<String, Object> gatilhoData = new HashMap<>();
        gatilhoData.put("tipo", "SITUACAO");
        gatilhoData.put("descricao", "Trabalho estressante");
        gatilhoData.put("impacto", 9);
        gatilhoData.put("emocaoAssociada", "ansiedade");
        gatilhoData.put("contexto", "Mencionado várias vezes");
        gatilhoData.put("positivo", false);
        gatilhosData.add(gatilhoData);
        memoriaData.put("gatilhos", gatilhosData);

        Trigger existingTrigger = new Trigger(userId, TipoGatilho.SITUACAO, "Trabalho estressante", 7, 1, "ansiedade", "contexto", false);

        when(triggerRepository.findByUsuarioIdAndDescricaoSimilar(userId, "Trabalho estressante"))
                .thenReturn(Optional.of(existingTrigger));
        when(triggerRepository.save(any(Trigger.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompletableFuture<Void> future = triggerService.processarGatilhos(userId, memoriaData);

        future.join();

        verify(triggerRepository).save(existingTrigger);
        assertEquals(2, existingTrigger.getFrequencia());
    }

    @Test
    @DisplayName("processarGatilhos - Não deve processar quando não há gatilhos")
    void testProcessarGatilhosEmpty() {
        memoriaData.put("gatilhos", new ArrayList<>());

        CompletableFuture<Void> future = triggerService.processarGatilhos(userId, memoriaData);

        future.join();

        verify(triggerRepository, never()).save(any(Trigger.class));
    }

    @Test
    @DisplayName("getGatilhos - Deve retornar lista de gatilhos ordenados por impacto")
    void testGetGatilhos() {
        List<Trigger> triggers = new ArrayList<>();
        triggers.add(new Trigger(userId, TipoGatilho.SITUACAO, "Gatilho 1", 8, 1, "ansiedade", "contexto", false));
        triggers.add(new Trigger(userId, TipoGatilho.PESSOA, "Gatilho 2", 6, 1, "tristeza", "contexto", false));

        when(triggerRepository.findByUsuarioIdAndAtivoTrueOrderByImpactoDesc(userId))
                .thenReturn(triggers);

        List<Trigger> result = triggerService.getGatilhos(userId);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(triggerRepository).findByUsuarioIdAndAtivoTrueOrderByImpactoDesc(userId);
    }

    @Test
    @DisplayName("getGatilhosPositivos - Deve retornar apenas gatilhos positivos")
    void testGetGatilhosPositivos() {
        List<Trigger> triggers = new ArrayList<>();
        triggers.add(new Trigger(userId, TipoGatilho.SITUACAO, "Gatilho positivo", 7, 1, "alegria", "contexto", true));

        when(triggerRepository.findByUsuarioIdAndPositivoAndAtivoTrue(userId, true))
                .thenReturn(triggers);

        List<Trigger> result = triggerService.getGatilhosPositivos(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getPositivo());
    }

    @Test
    @DisplayName("getGatilhosNegativos - Deve retornar apenas gatilhos negativos")
    void testGetGatilhosNegativos() {
        List<Trigger> triggers = new ArrayList<>();
        triggers.add(new Trigger(userId, TipoGatilho.SITUACAO, "Gatilho negativo", 8, 1, "ansiedade", "contexto", false));

        when(triggerRepository.findByUsuarioIdAndPositivoAndAtivoTrue(userId, false))
                .thenReturn(triggers);

        List<Trigger> result = triggerService.getGatilhosNegativos(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.get(0).getPositivo());
    }
}

