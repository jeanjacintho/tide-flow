package br.jeanjacintho.tideflow.ai_service.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente para interagir com a API do Google Gemini.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "llm.provider", havingValue = "gemini")
public class GeminiClient implements LLMClient {

    private static final Logger logger = LoggerFactory.getLogger(GeminiClient.class);

    private final WebClient webClient;
    private final String apiKey;
    private final String modelName;
    private final int timeout;

    public GeminiClient(WebClient geminiWebClient,
                       @Value("${gemini.api.key}") String apiKey,
                       @Value("${gemini.model.name:gemini-2.0-flash}") String modelName,
                       @Value("${timeout}") int timeout) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Gemini API key is required. Please set GEMINI_API_KEY environment variable or gemini.api.key property."
            );
        }
        this.webClient = geminiWebClient;
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.timeout = timeout;
    }

    @Override
    public Mono<String> generateResponse(String prompt) {
        Map<String, Object> requestBody = buildGenerateRequest(prompt);

        return webClient.post()
                .uri("/v1beta/models/{model}:generateContent", modelName)
                .header("X-goog-api-key", apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(timeout))
                .map(this::extractTextFromResponse)
                .doOnError(this::logError)
                .onErrorReturn("Desculpe, não consegui processar sua mensagem no momento.");
    }

    @Override
    public Mono<String> chatWithHistory(List<Map<String, String>> messages) {
        Map<String, Object> requestBody = buildChatRequest(messages);

        return webClient.post()
                .uri("/v1beta/models/{model}:generateContent", modelName)
                .header("X-goog-api-key", apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(timeout))
                .map(this::extractTextFromResponse)
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException ex = (WebClientResponseException) error;
                        // Não loga requestBody completo para evitar vazar informações sensíveis
                        logger.error("Erro na API do Gemini (Status: {}): {}", 
                                ex.getStatusCode(), sanitizeErrorResponse(ex.getResponseBodyAsString()));
                    } else {
                        logger.error("Erro ao chamar API do Gemini: {}", error.getMessage());
                    }
                })
                .onErrorReturn("Desculpe, não consegui processar sua mensagem no momento.");
    }

    @Override
    public Mono<String> extractMemories(String userMessage, String aiResponse) {
        String prompt = String.format(
            "Analise a seguinte conversa e extraia informações importantes que devem ser lembradas sobre o usuário. " +
            "Identifique: fatos pessoais, preferências, objetivos, eventos futuros, relacionamentos importantes.\n\n" +
            "Usuário: %s\n\n" +
            "IA: %s\n\n" +
            "Retorne APENAS um JSON válido no seguinte formato (sem markdown, sem texto adicional):\n" +
            "{\n" +
            "  \"memorias\": [\n" +
            "    {\n" +
            "      \"tipo\": \"FATO_PESSOAL|PREFERENCIA|OBJETIVO|EVENTO|RELACIONAMENTO\",\n" +
            "      \"conteudo\": \"descrição clara e concisa da informação\",\n" +
            "      \"relevancia\": 0-100,\n" +
            "      \"tags\": [\"tag1\", \"tag2\"]\n" +
            "    }\n" +
            "  ],\n" +
            "  \"gatilhos\": [\n" +
            "    {\n" +
            "      \"tipo\": \"PESSOA|EVENTO|LUGAR|SITUACAO\",\n" +
            "      \"descricao\": \"descrição clara do gatilho\",\n" +
            "      \"impacto\": 1-10,\n" +
            "      \"emocaoAssociada\": \"emoção que o gatilho causa\",\n" +
            "      \"contexto\": \"contexto onde o gatilho ocorre\",\n" +
            "      \"positivo\": true/false\n" +
            "    }\n" +
            "  ]\n" +
            "}\n\n" +
            "Gatilhos são fatores que afetam o estado emocional do usuário. " +
            "Gatilhos positivos melhoram o humor, gatilhos negativos pioram. " +
            "Impacto: 1-3 (leve), 4-6 (moderado), 7-10 (forte). " +
            "Se não houver informações importantes, retorne: {\"memorias\": [], \"gatilhos\": []}",
            userMessage, aiResponse
        );

        Map<String, Object> requestBody = buildGenerateRequest(prompt);

        return webClient.post()
                .uri("/v1beta/models/{model}:generateContent", modelName)
                .header("X-goog-api-key", apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(timeout * 2))
                .map(this::extractTextFromResponse)
                .map(response -> response != null ? response : "{\"memorias\": []}")
                .doOnError(this::logError)
                .onErrorReturn("{\"memorias\": []}");
    }

    @Override
    public Mono<String> generateProactiveQuestion(String memoriaConteudo, String memoriaTipo) {
        String prompt = String.format(
            "Com base nesta memória sobre o usuário, gere uma pergunta natural e empática que mostre que você se lembra dele.\n\n" +
            "Memória: %s (Tipo: %s)\n\n" +
            "Gere uma pergunta curta, natural e empática. Não mencione que está consultando uma memória. " +
            "Apenas faça a pergunta como se fosse uma continuação natural da conversa.",
            memoriaConteudo, memoriaTipo
        );

        Map<String, Object> requestBody = buildGenerateRequest(prompt);

        return webClient.post()
                .uri("/v1beta/models/{model}:generateContent", modelName)
                .header("X-goog-api-key", apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(timeout))
                .map(this::extractTextFromResponse)
                .map(response -> response != null ? response.trim() : "")
                .doOnError(this::logError)
                .onErrorReturn("");
    }

    @Override
    public Mono<String> extractEmotionalAnalysis(String userMessage) {
        String prompt = String.format(
            "Analise a seguinte mensagem do usuário e extraia informações sobre suas emoções.\n\n" +
            "Mensagem: %s\n\n" +
            "Retorne APENAS um JSON válido no seguinte formato (sem markdown, sem texto adicional):\n" +
            "{\n" +
            "  \"primaryEmotional\": \"tristeza|ansiedade|alegria|raiva|medo|neutro\",\n" +
            "  \"intensity\": 0-100,\n" +
            "  \"triggers\": [\"trigger1\", \"trigger2\"],\n" +
            "  \"context\": \"breve contexto sobre a situação emocional\",\n" +
            "  \"suggestion\": \"sugestão curta e empática\"\n" +
            "}\n\n" +
            "Seja preciso na análise emocional. Considere o tom, palavras-chave e contexto da mensagem.",
            userMessage
        );

        Map<String, Object> requestBody = buildGenerateRequest(prompt);

        return webClient.post()
                .uri("/v1beta/models/{model}:generateContent", modelName)
                .header("X-goog-api-key", apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(timeout))
                .map(this::extractTextFromResponse)
                .map(response -> response != null ? response : "{}")
                .doOnError(this::logError)
                .onErrorReturn("{}");
    }

    @Override
    public Mono<String> extractEmotionalAnalysisAndMemories(String userMessage, String aiResponse) {
        String prompt = String.format(
            "Analise a seguinte conversa e extraia duas informações importantes:\n\n" +
            "1. Análise emocional da mensagem do usuário\n" +
            "2. Memórias importantes que devem ser lembradas sobre o usuário\n\n" +
            "Usuário: %s\n\n" +
            "IA: %s\n\n" +
            "Retorne APENAS um JSON válido no seguinte formato (sem markdown, sem texto adicional):\n" +
            "{\n" +
            "  \"analiseEmocional\": {\n" +
            "    \"primaryEmotional\": \"tristeza|ansiedade|alegria|raiva|medo|neutro\",\n" +
            "    \"intensity\": 0-100,\n" +
            "    \"triggers\": [\"trigger1\", \"trigger2\"],\n" +
            "    \"context\": \"breve contexto sobre a situação emocional\",\n" +
            "    \"suggestion\": \"sugestão curta e empática\"\n" +
            "  },\n" +
            "  \"memorias\": [\n" +
            "    {\n" +
            "      \"tipo\": \"FATO_PESSOAL|PREFERENCIA|OBJETIVO|EVENTO|RELACIONAMENTO\",\n" +
            "      \"conteudo\": \"descrição clara e concisa da informação\",\n" +
            "      \"relevancia\": 0-100,\n" +
            "      \"tags\": [\"tag1\", \"tag2\"]\n" +
            "    }\n" +
            "  ],\n" +
            "  \"gatilhos\": [\n" +
            "    {\n" +
            "      \"tipo\": \"PESSOA|EVENTO|LUGAR|SITUACAO\",\n" +
            "      \"descricao\": \"descrição clara do gatilho\",\n" +
            "      \"impacto\": 1-10,\n" +
            "      \"emocaoAssociada\": \"emoção que o gatilho causa\",\n" +
            "      \"contexto\": \"contexto onde o gatilho ocorre\",\n" +
            "      \"positivo\": true/false\n" +
            "    }\n" +
            "  ]\n" +
            "}\n\n" +
            "INSTRUÇÕES:\n" +
            "- Para análise emocional: Seja preciso, considere o tom, palavras-chave e contexto da mensagem do usuário.\n" +
            "- Para memórias: Identifique fatos pessoais, preferências, objetivos, eventos futuros, relacionamentos importantes.\n" +
            "- Para gatilhos: Identifique fatores que afetam o estado emocional (positivos melhoram humor, negativos pioram). Impacto: 1-3 (leve), 4-6 (moderado), 7-10 (forte).\n" +
            "Se não houver informações importantes, retorne arrays vazios mas mantenha a estrutura JSON.",
            userMessage, aiResponse
        );

        Map<String, Object> requestBody = buildGenerateRequest(prompt);

        return webClient.post()
                .uri("/v1beta/models/{model}:generateContent", modelName)
                .header("X-goog-api-key", apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(timeout * 2))
                .map(this::extractTextFromResponse)
                .map(response -> response != null ? response : "{\"analiseEmocional\": {}, \"memorias\": [], \"gatilhos\": []}")
                .doOnError(this::logError)
                .onErrorReturn("{\"analiseEmocional\": {}, \"memorias\": [], \"gatilhos\": []}");
    }

    /**
     * Constrói o request body para generateContent do Gemini.
     */
    private Map<String, Object> buildGenerateRequest(String prompt) {
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));

        Map<String, Object> content = new HashMap<>();
        content.put("parts", parts);

        List<Map<String, Object>> contents = new ArrayList<>();
        contents.add(content);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", contents);

        // Configurações de geração
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("topK", 40);
        generationConfig.put("topP", 0.95);
        generationConfig.put("maxOutputTokens", 8192);
        requestBody.put("generationConfig", generationConfig);

        return requestBody;
    }

    /**
     * Constrói o request body para chat com histórico.
     * O Gemini não aceita mensagens "system", então incorporamos o system prompt na primeira mensagem do usuário.
     */
    private Map<String, Object> buildChatRequest(List<Map<String, String>> messages) {
        List<Map<String, Object>> contents = new ArrayList<>();
        String systemPrompt = null;
        boolean isFirstUserMessage = true;

        for (Map<String, String> message : messages) {
            String role = message.get("role");
            String contentText = message.get("content");

            if (contentText == null || contentText.trim().isEmpty()) {
                continue; // Pula mensagens vazias
            }

            // Gemini não aceita mensagens "system", então guardamos para incorporar na primeira mensagem do usuário
            if ("system".equals(role)) {
                systemPrompt = contentText;
                continue;
            }

            // Gemini usa "user" e "model" ao invés de "assistant"
            String geminiRole;
            if ("assistant".equals(role)) {
                geminiRole = "model";
            } else {
                geminiRole = "user";
            }

            // Incorpora system prompt na primeira mensagem do usuário
            String finalContent = contentText;
            if (isFirstUserMessage && systemPrompt != null && "user".equals(geminiRole)) {
                finalContent = systemPrompt + "\n\n" + contentText;
                isFirstUserMessage = false;
            } else if ("user".equals(geminiRole)) {
                isFirstUserMessage = false;
            }

            List<Map<String, Object>> parts = new ArrayList<>();
            parts.add(Map.of("text", finalContent));

            Map<String, Object> contentMap = new HashMap<>();
            contentMap.put("role", geminiRole);
            contentMap.put("parts", parts);
            contents.add(contentMap);
        }

        // Se não houver mensagens válidas, retorna erro
        if (contents.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma mensagem válida para enviar ao Gemini");
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", contents);

        // Configurações de geração
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("topK", 40);
        generationConfig.put("topP", 0.95);
        generationConfig.put("maxOutputTokens", 8192);
        requestBody.put("generationConfig", generationConfig);

        return requestBody;
    }

    /**
     * Loga erros da API do Gemini de forma segura (sanitiza informações sensíveis).
     */
    private void logError(Throwable error) {
        if (error instanceof WebClientResponseException) {
            WebClientResponseException ex = (WebClientResponseException) error;
            logger.error("Erro na API do Gemini (Status: {}): {}", 
                    ex.getStatusCode(), sanitizeErrorResponse(ex.getResponseBodyAsString()));
        } else {
            logger.error("Erro ao chamar API do Gemini: {}", error.getMessage());
        }
    }

    /**
     * Sanitiza mensagens de erro para remover informações sensíveis (como API keys).
     */
    private String sanitizeErrorResponse(String errorResponse) {
        if (errorResponse == null || errorResponse.isEmpty()) {
            return "Sem detalhes do erro";
        }
        
        // Remove possíveis vazamentos de API key
        String sanitized = errorResponse
                .replaceAll("(?i)api[_-]?key", "***")
                .replaceAll("(?i)X-goog-api-key", "***")
                .replaceAll("AIzaSy[A-Za-z0-9_-]{35}", "***REDACTED***");
        
        // Limita o tamanho para evitar logs muito grandes
        if (sanitized.length() > 500) {
            sanitized = sanitized.substring(0, 497) + "...";
        }
        
        return sanitized;
    }

    /**
     * Extrai o texto da resposta do Gemini.
     */
    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return "Sem resposta";
            }

            Map<String, Object> candidate = candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) candidate.get("content");
            if (content == null) {
                return "Sem resposta";
            }

            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) {
                return "Sem resposta";
            }

            Map<String, Object> part = parts.get(0);
            Object text = part.get("text");
            return text != null ? text.toString() : "Sem resposta";
        } catch (Exception e) {
            return "Erro ao processar resposta";
        }
    }
}

