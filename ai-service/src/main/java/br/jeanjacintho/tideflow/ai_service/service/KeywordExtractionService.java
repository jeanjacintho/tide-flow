package br.jeanjacintho.tideflow.ai_service.service;

import br.jeanjacintho.tideflow.ai_service.model.EmotionalAnalysis;
import br.jeanjacintho.tideflow.ai_service.model.MessageRole;
import br.jeanjacintho.tideflow.ai_service.repository.ConversationMessageRepository;
import br.jeanjacintho.tideflow.ai_service.repository.EmotionalAnalysisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class KeywordExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(KeywordExtractionService.class);

    private static final int MIN_KEYWORD_LENGTH = 3;
    private static final int MAX_KEYWORDS = 20;
    private static final Pattern WORD_PATTERN = Pattern.compile("\\b\\w+\\b");

    private static final Set<String> STOP_WORDS = Set.of(
        "o", "a", "os", "as", "um", "uma", "uns", "umas",
        "de", "do", "da", "dos", "das", "em", "no", "na", "nos", "nas",
        "para", "por", "com", "sem", "sob", "sobre", "entre", "até",
        "que", "qual", "quais", "quando", "onde", "como", "porque",
        "é", "são", "foi", "ser", "estar", "ter", "haver",
        "eu", "você", "ele", "ela", "nós", "eles", "elas",
        "me", "te", "se", "vos", "lhe", "lhes",
        "meu", "minha", "teu", "tua", "seu", "sua", "nosso", "nossa",
        "isso", "isto", "aquilo", "este", "esta", "esse", "essa",
        "muito", "mais", "menos", "pouco", "bastante",
        "já", "ainda", "sempre", "nunca", "agora", "hoje", "ontem", "amanhã",
        "também", "tampouco", "nem", "ou", "e", "mas", "porém"
    );

    private final ConversationMessageRepository messageRepository;
    private final EmotionalAnalysisRepository emotionalAnalysisRepository;

    public KeywordExtractionService(
            ConversationMessageRepository messageRepository,
            EmotionalAnalysisRepository emotionalAnalysisRepository) {
        this.messageRepository = messageRepository;
        this.emotionalAnalysisRepository = emotionalAnalysisRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Integer> extractKeywordsFromDepartment(UUID departmentId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        logger.info("Extraindo palavras-chave para departamento {} no período {} - {}",
            departmentId, startDateTime, endDateTime);

        List<EmotionalAnalysis> analyses = emotionalAnalysisRepository.findByDepartmentIdAndDateRange(
            departmentId, startDateTime, endDateTime
        );

        if (analyses.isEmpty()) {
            logger.warn("Nenhuma análise encontrada para departamento {}", departmentId);
            return new HashMap<>();
        }

        Map<String, Integer> keywordFrequency = new HashMap<>();
        Set<UUID> processedConversations = new HashSet<>();

        for (EmotionalAnalysis analysis : analyses) {
            if (analysis.getConversationId() != null && !processedConversations.contains(analysis.getConversationId())) {
                List<br.jeanjacintho.tideflow.ai_service.model.ConversationMessage> messages =
                    messageRepository.findByConversationIdOrderBySequenceNumberAsc(analysis.getConversationId());

                for (br.jeanjacintho.tideflow.ai_service.model.ConversationMessage message : messages) {
                    if (message.getRole() == MessageRole.USER) {
                        extractKeywordsFromText(message.getContent(), keywordFrequency);
                    }
                }

                processedConversations.add(analysis.getConversationId());
            }
        }

        return filterTopKeywords(keywordFrequency);
    }

    public void extractKeywordsFromText(String text, Map<String, Integer> keywordFrequency) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        String normalizedText = normalizeText(text);
        WORD_PATTERN.matcher(normalizedText).results()
            .map(match -> match.group().toLowerCase())
            .filter(word -> word.length() >= MIN_KEYWORD_LENGTH)
            .filter(word -> !STOP_WORDS.contains(word))
            .forEach(word -> keywordFrequency.merge(word, 1, Integer::sum));
    }

    private Map<String, Integer> filterTopKeywords(Map<String, Integer> keywordFrequency) {
        return keywordFrequency.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(MAX_KEYWORDS)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }

    private String normalizeText(String text) {
        return text.toLowerCase()
            .replaceAll("[áàâãä]", "a")
            .replaceAll("[éèêë]", "e")
            .replaceAll("[íìîï]", "i")
            .replaceAll("[óòôõö]", "o")
            .replaceAll("[úùûü]", "u")
            .replaceAll("[ç]", "c")
            .replaceAll("[^\\w\\s]", " ");
    }

    @Transactional(readOnly = true)
    public Double calculateSentimentScore(UUID departmentId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        logger.info("Calculando score de sentimento para departamento {} no período {} - {}",
            departmentId, startDateTime, endDateTime);

        List<EmotionalAnalysis> analyses = emotionalAnalysisRepository.findByDepartmentIdAndDateRange(
            departmentId, startDateTime, endDateTime
        );

        if (analyses.isEmpty()) {
            return null;
        }

        Map<String, Integer> emotionScores = Map.ofEntries(
            Map.entry("alegria", 80),
            Map.entry("felicidade", 85),
            Map.entry("contentamento", 75),
            Map.entry("ansiedade", 30),
            Map.entry("tristeza", 20),
            Map.entry("raiva", 15),
            Map.entry("medo", 25),
            Map.entry("estresse", 25),
            Map.entry("frustração", 20),
            Map.entry("calma", 70),
            Map.entry("paz", 75),
            Map.entry("satisfação", 75)
        );

        double totalScore = 0.0;
        int count = 0;

        for (EmotionalAnalysis analysis : analyses) {
            String emotion = analysis.getPrimaryEmotional();
            if (emotion != null) {
                Integer baseScore = emotionScores.getOrDefault(emotion.toLowerCase(), 50);
                double intensityFactor = analysis.getIntensity() != null ? analysis.getIntensity() / 100.0 : 0.5;
                double weightedScore = baseScore * intensityFactor;
                totalScore += weightedScore;
                count++;
            }
        }

        return count > 0 ? totalScore / count : null;
    }

}
