package br.jeanjacintho.tideflow.ai_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.jeanjacintho.tideflow.ai_service.model.ConversationMessage;
import br.jeanjacintho.tideflow.ai_service.model.EmotionalPattern;
import br.jeanjacintho.tideflow.ai_service.model.MessageRole;
import br.jeanjacintho.tideflow.ai_service.model.TipoPadrao;
import br.jeanjacintho.tideflow.ai_service.repository.ConversationMessageRepository;
import br.jeanjacintho.tideflow.ai_service.repository.EmotionalPatternRepository;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("PatternAnalysisService Tests")
@SuppressWarnings("unchecked")
class PatternAnalysisServiceTest {

    @Mock
    private EmotionalPatternRepository patternRepository;

    @Mock
    private ConversationMessageRepository messageRepository;

    @InjectMocks
    private PatternAnalysisService patternAnalysisService;

    private String userId;
    private List<ConversationMessage> messages;

    @BeforeEach
    void setUp() {
        userId = "user-123";
        messages = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            ConversationMessage msg = new ConversationMessage(MessageRole.USER, "Mensagem " + i, i + 1);
            msg.setCreatedAt(LocalDateTime.now().minusDays(i));
            messages.add(msg);
        }
    }

    @Test
    @DisplayName("analisarPadroesTemporais - Deve analisar padrões quando há mensagens suficientes")
    void testAnalisarPadroesTemporaisSuccess() {
        when(messageRepository.findByUserIdAndRoleOrderByCreatedAtAsc(userId, MessageRole.USER))
                .thenReturn(messages);
        when(patternRepository.findByUsuarioIdAndPadraoAndTipo(anyString(), anyString(), any(TipoPadrao.class)))
                .thenReturn(null);
        when(patternRepository.saveAll(any(List.class))).thenAnswer(invocation -> invocation.getArgument(0));

        patternAnalysisService.analisarPadroesTemporais(userId);

        verify(messageRepository).findByUserIdAndRoleOrderByCreatedAtAsc(userId, MessageRole.USER);
        verify(patternRepository).saveAll(any(Iterable.class));
    }

    @Test
    @DisplayName("analisarPadroesTemporais - Não deve analisar quando há poucas mensagens")
    void testAnalisarPadroesTemporaisInsufficientMessages() {
        List<ConversationMessage> fewMessages = new ArrayList<>();
        fewMessages.add(new ConversationMessage(MessageRole.USER, "Mensagem 1", 1));

        when(messageRepository.findByUserIdAndRoleOrderByCreatedAtAsc(userId, MessageRole.USER))
                .thenReturn(fewMessages);

        patternAnalysisService.analisarPadroesTemporais(userId);

        verify(messageRepository).findByUserIdAndRoleOrderByCreatedAtAsc(userId, MessageRole.USER);
        verify(patternRepository, org.mockito.Mockito.never()).saveAll(any(Iterable.class));
    }

    @Test
    @DisplayName("analisarPadroesTemporais - Não deve analisar quando não há mensagens")
    void testAnalisarPadroesTemporaisNoMessages() {
        when(messageRepository.findByUserIdAndRoleOrderByCreatedAtAsc(userId, MessageRole.USER))
                .thenReturn(new ArrayList<>());

        patternAnalysisService.analisarPadroesTemporais(userId);

        verify(messageRepository).findByUserIdAndRoleOrderByCreatedAtAsc(userId, MessageRole.USER);
        verify(patternRepository, org.mockito.Mockito.never()).saveAll(any(Iterable.class));
    }

    @Test
    @DisplayName("getPadroes - Deve retornar lista de padrões ordenados por confiança")
    void testGetPadroes() {
        List<EmotionalPattern> patterns = new ArrayList<>();
        EmotionalPattern pattern1 = new EmotionalPattern(userId, TipoPadrao.SEMANAL, "Segunda-feira", null, null, 80.0, 5);
        EmotionalPattern pattern2 = new EmotionalPattern(userId, TipoPadrao.DIARIO, "Manhã", null, null, 70.0, 4);
        patterns.add(pattern1);
        patterns.add(pattern2);

        when(patternRepository.findByUsuarioIdAndAtivoTrueOrderByConfiancaDesc(userId))
                .thenReturn(patterns);

        List<EmotionalPattern> result = patternAnalysisService.getPadroes(userId);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(patternRepository).findByUsuarioIdAndAtivoTrueOrderByConfiancaDesc(userId);
    }
}
