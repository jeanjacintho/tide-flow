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
 * Cliente para interagir com a API do OpenRouter.
 * Suporta múltiplos modelos através do OpenRouter, incluindo x-ai/grok-4.1-fast.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "llm.provider", havingValue = "openrouter")
public class OpenRouterClient implements LLMClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenRouterClient.class);

    private final WebClient webClient;
    private final String apiKey;
    private final String modelName;
    private final int timeout;

    public OpenRouterClient(WebClient openRouterWebClient,
                           @Value("${openrouter.api.key}") String apiKey,
                           @Value("${openrouter.model.name:x-ai/grok-4.1-fast}") String modelName,
                           @Value("${timeout:60000}") int timeout) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "OpenRouter API key is required. Please set OPENROUTER_API_KEY environment variable or openrouter.api.key property."
            );
        }
        this.webClient = openRouterWebClient;
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.timeout = timeout;
    }

    @Override
    public Mono<String> generateResponse(String prompt) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", prompt));
        
        return chatWithHistory(messages);
    }

    @Override
    public Mono<String> chatWithHistory(List<Map<String, String>> messages) {
        Map<String, Object> requestBody = buildChatRequest(messages);

        return webClient.post()
                .uri("/api/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("HTTP-Referer", "https://github.com/jeanjacintho/tide-flow")
                .header("X-Title", "Tide Flow")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(timeout))
                .map(this::extractTextFromResponse)
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException ex = (WebClientResponseException) error;
                        logger.error("Erro na API do OpenRouter (Status: {}): {}", 
                                ex.getStatusCode(), sanitizeErrorResponse(ex.getResponseBodyAsString()));
                    } else {
                        logger.error("Erro ao chamar API do OpenRouter: {}", error.getMessage());
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

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", prompt));

        Map<String, Object> requestBody = buildChatRequest(messages);

        return webClient.post()
                .uri("/api/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("HTTP-Referer", "https://github.com/jeanjacintho/tide-flow")
                .header("X-Title", "Tide Flow")
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

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", prompt));

        Map<String, Object> requestBody = buildChatRequest(messages);

        return webClient.post()
                .uri("/api/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("HTTP-Referer", "https://github.com/jeanjacintho/tide-flow")
                .header("X-Title", "Tide Flow")
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

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", prompt));

        Map<String, Object> requestBody = buildChatRequest(messages);

        return webClient.post()
                .uri("/api/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("HTTP-Referer", "https://github.com/jeanjacintho/tide-flow")
                .header("X-Title", "Tide Flow")
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

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", prompt));

        Map<String, Object> requestBody = buildChatRequest(messages);

        return webClient.post()
                .uri("/api/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("HTTP-Referer", "https://github.com/jeanjacintho/tide-flow")
                .header("X-Title", "Tide Flow")
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
     * Constrói o request body para chat/completions do OpenRouter (formato OpenAI).
     */
    private Map<String, Object> buildChatRequest(List<Map<String, String>> messages) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("messages", messages);

        // Configurações de geração
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("temperature", 0.7);
        parameters.put("max_tokens", 8192);
        requestBody.putAll(parameters);

        return requestBody;
    }

    /**
     * Loga erros da API do OpenRouter de forma segura (sanitiza informações sensíveis).
     */
    private void logError(Throwable error) {
        if (error instanceof WebClientResponseException) {
            WebClientResponseException ex = (WebClientResponseException) error;
            logger.error("Erro na API do OpenRouter (Status: {}): {}", 
                    ex.getStatusCode(), sanitizeErrorResponse(ex.getResponseBodyAsString()));
        } else {
            logger.error("Erro ao chamar API do OpenRouter: {}", error.getMessage());
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
                .replaceAll("(?i)authorization", "***")
                .replaceAll("sk-or-v1-[A-Za-z0-9_-]+", "***REDACTED***");
        
        // Limita o tamanho para evitar logs muito grandes
        if (sanitized.length() > 500) {
            sanitized = sanitized.substring(0, 497) + "...";
        }
        
        return sanitized;
    }

    /**
     * Extrai o texto da resposta do OpenRouter (formato OpenAI).
     */
    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                return "Sem resposta";
            }

            Map<String, Object> choice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) choice.get("message");
            if (message == null) {
                return "Sem resposta";
            }

            Object content = message.get("content");
            return content != null ? content.toString() : "Sem resposta";
        } catch (Exception e) {
            logger.error("Erro ao processar resposta do OpenRouter: {}", e.getMessage());
            return "Erro ao processar resposta";
        }
    }
}

