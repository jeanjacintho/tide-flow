package br.jeanjacintho.tideflow.ai_service.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.jeanjacintho.tideflow.ai_service.model.ConversationMessage;
import br.jeanjacintho.tideflow.ai_service.model.EmotionalAnalysis;
import br.jeanjacintho.tideflow.ai_service.model.MessageRole;
import br.jeanjacintho.tideflow.ai_service.model.Trigger;
import br.jeanjacintho.tideflow.ai_service.model.TipoGatilho;
import br.jeanjacintho.tideflow.ai_service.repository.ConversationMessageRepository;
import br.jeanjacintho.tideflow.ai_service.repository.EmotionalAnalysisRepository;
import br.jeanjacintho.tideflow.ai_service.repository.TriggerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
@DisplayName("TriggerAnalysisService Tests")
class TriggerAnalysisServiceTest {

    @Mock
    private TriggerRepository triggerRepository;

    @Mock
    private ConversationMessageRepository messageRepository;

    @Mock
    private EmotionalAnalysisRepository emotionalAnalysisRepository;

    @InjectMocks
    private TriggerAnalysisService triggerAnalysisService;

    private String userId;
    private List<Trigger> triggers;
    private List<ConversationMessage> messages;

    @BeforeEach
    void setUp() {
        userId = "user-123";

        triggers = new ArrayList<>();
        Trigger trigger = new Trigger(userId, TipoGatilho.SITUACAO, "Trabalho estressante", 7, 1, "ansiedade", "contexto", false);
        triggers.add(trigger);

        messages = new ArrayList<>();
        ConversationMessage msg = new ConversationMessage(MessageRole.USER, "Estou estressado com o trabalho", 1);
        msg.setId(UUID.randomUUID());
        msg.setCreatedAt(LocalDateTime.now());
        messages.add(msg);
    }

    @Test
    @DisplayName("analisarCorrelacaoGatilhoEmocao - Deve analisar correlação quando há gatilhos e mensagens")
    void testAnalisarCorrelacaoGatilhoEmocaoSuccess() {
        List<ConversationMessage> multipleMessages = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ConversationMessage msg = new ConversationMessage(MessageRole.USER, "Estou estressado com o trabalho " + i, i + 1);
            msg.setId(UUID.randomUUID());
            msg.setCreatedAt(LocalDateTime.now().minusDays(i));
            multipleMessages.add(msg);
        }

        when(triggerRepository.findByUsuarioIdAndAtivoTrueOrderByImpactoDesc(userId))
                .thenReturn(triggers);
        when(messageRepository.findByUserIdAndRoleOrderByCreatedAtAsc(userId, MessageRole.USER))
                .thenReturn(multipleMessages);

        EmotionalAnalysis analysis = new EmotionalAnalysis("ansiedade", 75, new ArrayList<>(), "contexto", "sugestão");
        when(emotionalAnalysisRepository.findByMessageId(any(UUID.class)))
                .thenReturn(Optional.of(analysis));
        when(triggerRepository.save(any(Trigger.class))).thenAnswer(invocation -> invocation.getArgument(0));

        triggerAnalysisService.analisarCorrelacaoGatilhoEmocao(userId);

        verify(triggerRepository).findByUsuarioIdAndAtivoTrueOrderByImpactoDesc(userId);
        verify(messageRepository).findByUserIdAndRoleOrderByCreatedAtAsc(userId, MessageRole.USER);
        verify(triggerRepository, org.mockito.Mockito.atMostOnce()).save(any(Trigger.class));
    }

    @Test
    @DisplayName("analisarCorrelacaoGatilhoEmocao - Não deve analisar quando não há gatilhos")
    void testAnalisarCorrelacaoGatilhoEmocaoNoTriggers() {
        when(triggerRepository.findByUsuarioIdAndAtivoTrueOrderByImpactoDesc(userId))
                .thenReturn(new ArrayList<>());

        triggerAnalysisService.analisarCorrelacaoGatilhoEmocao(userId);

        verify(triggerRepository).findByUsuarioIdAndAtivoTrueOrderByImpactoDesc(userId);
        verify(messageRepository, org.mockito.Mockito.never()).findByUserIdAndRoleOrderByCreatedAtAsc(anyString(), any(MessageRole.class));
    }

    @Test
    @DisplayName("analisarCorrelacaoGatilhoEmocao - Deve tratar erro graciosamente")
    void testAnalisarCorrelacaoGatilhoEmocaoHandlesError() {
        when(triggerRepository.findByUsuarioIdAndAtivoTrueOrderByImpactoDesc(userId))
                .thenThrow(new RuntimeException("Database error"));

        triggerAnalysisService.analisarCorrelacaoGatilhoEmocao(userId);
    }
}
