package br.jeanjacintho.tideflow.ai_service.service;

import br.jeanjacintho.tideflow.ai_service.model.ConversationMessage;
import br.jeanjacintho.tideflow.ai_service.model.EmotionalPattern;
import br.jeanjacintho.tideflow.ai_service.model.MessageRole;
import br.jeanjacintho.tideflow.ai_service.model.TipoPadrao;
import br.jeanjacintho.tideflow.ai_service.repository.ConversationMessageRepository;
import br.jeanjacintho.tideflow.ai_service.repository.EmotionalPatternRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PatternAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(PatternAnalysisService.class);
    private static final int MIN_OBSERVACOES_PADRAO = 3;
    private static final double MIN_CONFIANCA_PADRAO = 50.0;

    private final EmotionalPatternRepository patternRepository;
    private final ConversationMessageRepository messageRepository;

    public PatternAnalysisService(EmotionalPatternRepository patternRepository,
                                 ConversationMessageRepository messageRepository) {
        this.patternRepository = patternRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    @Async
    public void analisarPadroesTemporais(String userId) {
        logger.info("Iniciando análise de padrões temporais para usuário: {}", userId);

        try {

            List<ConversationMessage> userMessages = messageRepository
                    .findByUserIdAndRoleOrderByCreatedAtAsc(userId, MessageRole.USER);

            if (userMessages.isEmpty()) {
                logger.info("Nenhuma mensagem encontrada para análise de padrões");
                return;
            }

            List<MessageData> messageDataList = new ArrayList<>();
            for (ConversationMessage msg : userMessages) {
                messageDataList.add(new MessageData(msg.getCreatedAt()));
            }

            if (messageDataList.size() < MIN_OBSERVACOES_PADRAO) {
                logger.info("Poucas mensagens para análise de padrões: {}", messageDataList.size());
                return;
            }

            List<EmotionalPattern> patternsToSave = new ArrayList<>();
            patternsToSave.addAll(analisarPadroesSemanais(userId, messageDataList));
            patternsToSave.addAll(analisarPadroesHorarios(userId, messageDataList));
            patternsToSave.addAll(analisarPadroesMensais(userId, messageDataList));
            patternsToSave.addAll(analisarPadroesSazonais(userId, messageDataList));

            if (!patternsToSave.isEmpty()) {
                patternRepository.saveAll(patternsToSave);
                logger.info("Salvos {} padrões temporais", patternsToSave.size());
            }

            logger.info("Análise de padrões temporais concluída para usuário: {}", userId);
        } catch (Exception e) {
            logger.error("Erro ao analisar padrões temporais: {}", e.getMessage(), e);
        }
    }

    private List<EmotionalPattern> analisarPadroesSemanais(String userId, List<MessageData> messages) {
        List<EmotionalPattern> patternsToSave = new ArrayList<>();
        Map<DayOfWeek, List<MessageData>> porDiaSemana = messages.stream()
                .collect(Collectors.groupingBy(msg -> msg.data.getDayOfWeek()));

        for (Map.Entry<DayOfWeek, List<MessageData>> entry : porDiaSemana.entrySet()) {
            if (entry.getValue().size() >= MIN_OBSERVACOES_PADRAO) {
                DayOfWeek dia = entry.getKey();
                String nomeDia = getNomeDiaSemana(dia);

                double confianca = calcularConfianca(entry.getValue().size(), messages.size());

                if (confianca >= MIN_CONFIANCA_PADRAO) {
                    EmotionalPattern pattern = patternRepository.findByUsuarioIdAndPadraoAndTipo(
                            userId, nomeDia, TipoPadrao.SEMANAL);

                    if (pattern != null) {
                        pattern.incrementarOcorrencia();
                        pattern.setConfianca(confianca);
                    } else {
                        pattern = new EmotionalPattern(
                                userId,
                                TipoPadrao.SEMANAL,
                                nomeDia,
                                null,
                                null,
                                confianca,
                                entry.getValue().size()
                        );
                    }
                    patternsToSave.add(pattern);
                }
            }
        }
        return patternsToSave;
    }

    private List<EmotionalPattern> analisarPadroesHorarios(String userId, List<MessageData> messages) {
        List<EmotionalPattern> patternsToSave = new ArrayList<>();
        Map<String, List<MessageData>> porHorario = messages.stream()
                .collect(Collectors.groupingBy(msg -> {
                    int hora = msg.data.getHour();
                    if (hora >= 6 && hora < 12) return "Manhã";
                    if (hora >= 12 && hora < 18) return "Tarde";
                    if (hora >= 18 && hora < 22) return "Noite";
                    return "Madrugada";
                }));

        for (Map.Entry<String, List<MessageData>> entry : porHorario.entrySet()) {
            if (entry.getValue().size() >= MIN_OBSERVACOES_PADRAO) {
                String horario = entry.getKey();
                double confianca = calcularConfianca(entry.getValue().size(), messages.size());

                if (confianca >= MIN_CONFIANCA_PADRAO) {
                    EmotionalPattern pattern = patternRepository.findByUsuarioIdAndPadraoAndTipo(
                            userId, horario, TipoPadrao.DIARIO);

                    if (pattern != null) {
                        pattern.incrementarOcorrencia();
                        pattern.setConfianca(confianca);
                    } else {
                        pattern = new EmotionalPattern(
                                userId,
                                TipoPadrao.DIARIO,
                                horario,
                                null,
                                null,
                                confianca,
                                entry.getValue().size()
                        );
                    }
                    patternsToSave.add(pattern);
                }
            }
        }
        return patternsToSave;
    }

    private List<EmotionalPattern> analisarPadroesMensais(String userId, List<MessageData> messages) {
        List<EmotionalPattern> patternsToSave = new ArrayList<>();
        Map<Month, List<MessageData>> porMes = messages.stream()
                .collect(Collectors.groupingBy(msg -> msg.data.getMonth()));

        for (Map.Entry<Month, List<MessageData>> entry : porMes.entrySet()) {
            if (entry.getValue().size() >= MIN_OBSERVACOES_PADRAO) {
                Month mes = entry.getKey();
                String nomeMes = getNomeMes(mes);
                double confianca = calcularConfianca(entry.getValue().size(), messages.size());

                if (confianca >= MIN_CONFIANCA_PADRAO) {
                    EmotionalPattern pattern = patternRepository.findByUsuarioIdAndPadraoAndTipo(
                            userId, nomeMes, TipoPadrao.MENSAL);

                    if (pattern != null) {
                        pattern.incrementarOcorrencia();
                        pattern.setConfianca(confianca);
                    } else {
                        pattern = new EmotionalPattern(
                                userId,
                                TipoPadrao.MENSAL,
                                nomeMes,
                                null,
                                null,
                                confianca,
                                entry.getValue().size()
                        );
                    }
                    patternsToSave.add(pattern);
                }
            }
        }
        return patternsToSave;
    }

    private List<EmotionalPattern> analisarPadroesSazonais(String userId, List<MessageData> messages) {
        List<EmotionalPattern> patternsToSave = new ArrayList<>();
        Map<String, List<MessageData>> porEstacao = messages.stream()
                .collect(Collectors.groupingBy(msg -> {
                    Month mes = msg.data.getMonth();
                    if (mes == Month.DECEMBER || mes == Month.JANUARY || mes == Month.FEBRUARY) {
                        return "Verão";
                    } else if (mes == Month.MARCH || mes == Month.APRIL || mes == Month.MAY) {
                        return "Outono";
                    } else if (mes == Month.JUNE || mes == Month.JULY || mes == Month.AUGUST) {
                        return "Inverno";
                    } else {
                        return "Primavera";
                    }
                }));

        for (Map.Entry<String, List<MessageData>> entry : porEstacao.entrySet()) {
            if (entry.getValue().size() >= MIN_OBSERVACOES_PADRAO) {
                String estacao = entry.getKey();
                double confianca = calcularConfianca(entry.getValue().size(), messages.size());

                if (confianca >= MIN_CONFIANCA_PADRAO) {
                    EmotionalPattern pattern = patternRepository.findByUsuarioIdAndPadraoAndTipo(
                            userId, estacao, TipoPadrao.SAZONAL);

                    if (pattern != null) {
                        pattern.incrementarOcorrencia();
                        pattern.setConfianca(confianca);
                    } else {
                        pattern = new EmotionalPattern(
                                userId,
                                TipoPadrao.SAZONAL,
                                estacao,
                                null,
                                null,
                                confianca,
                                entry.getValue().size()
                        );
                    }
                    patternsToSave.add(pattern);
                }
            }
        }
        return patternsToSave;
    }

    private double calcularConfianca(int ocorrencias, int total) {

        double frequencia = (double) ocorrencias / total;

        return Math.min(100.0, frequencia * 100 * (1 + Math.log10(ocorrencias)));
    }

    private String getNomeDiaSemana(DayOfWeek dia) {
        Map<DayOfWeek, String> nomes = Map.of(
                DayOfWeek.MONDAY, "Segunda-feira",
                DayOfWeek.TUESDAY, "Terça-feira",
                DayOfWeek.WEDNESDAY, "Quarta-feira",
                DayOfWeek.THURSDAY, "Quinta-feira",
                DayOfWeek.FRIDAY, "Sexta-feira",
                DayOfWeek.SATURDAY, "Sábado",
                DayOfWeek.SUNDAY, "Domingo"
        );
        return nomes.getOrDefault(dia, dia.toString());
    }

    private String getNomeMes(Month mes) {
        Map<Month, String> nomes = new HashMap<>();
        nomes.put(Month.JANUARY, "Janeiro");
        nomes.put(Month.FEBRUARY, "Fevereiro");
        nomes.put(Month.MARCH, "Março");
        nomes.put(Month.APRIL, "Abril");
        nomes.put(Month.MAY, "Maio");
        nomes.put(Month.JUNE, "Junho");
        nomes.put(Month.JULY, "Julho");
        nomes.put(Month.AUGUST, "Agosto");
        nomes.put(Month.SEPTEMBER, "Setembro");
        nomes.put(Month.OCTOBER, "Outubro");
        nomes.put(Month.NOVEMBER, "Novembro");
        nomes.put(Month.DECEMBER, "Dezembro");
        return nomes.getOrDefault(mes, mes.toString());
    }

    @Transactional(readOnly = true)
    public List<EmotionalPattern> getPadroes(String userId) {
        return patternRepository.findByUsuarioIdAndAtivoTrueOrderByConfiancaDesc(userId);
    }

    private static class MessageData {
        LocalDateTime data;

        MessageData(LocalDateTime data) {
            this.data = data;
        }
    }
}
