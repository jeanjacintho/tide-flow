package br.jeanjacintho.tideflow.ai_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaClient implements LLMClient {

    private final WebClient webClient;
    private final String modelName;
    private final int timeout;

    public OllamaClient(WebClient ollamaWebClient,
                       @Value("${ollama.model.name}") String modelName,
                       @Value("${timeout}") int timeout) {
        this.webClient = ollamaWebClient;
        this.modelName = modelName;
        this.timeout = timeout;
    }

    @Override
    public Mono<String> generateResponse(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", modelName,
                "prompt", prompt,
                "stream", false
        );

        return webClient.post()
                .uri("/api/generate")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(timeout))
                .map(response -> {
                    Object responseObj = response.get("response");
                    return responseObj != null ? responseObj.toString() : "Sem resposta";
                })
                .onErrorReturn("Desculpe, não consegui processar sua mensagem no momento.");
    }

    @Override
    public Mono<String> chatWithHistory(List<Map<String, String>> messages) {
        Map<String, Object> requestBody = Map.of(
                "model", modelName,
                "messages", messages,
                "stream", false
        );

        return webClient.post()
                .uri("/api/chat")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(timeout))
                .map(response -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> messageObj = (Map<String, Object>) response.get("message");
                    if (messageObj != null) {
                        Object content = messageObj.get("content");
                        return content != null ? content.toString() : "Sem resposta";
                    }
                    return "Sem resposta";
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

        Map<String, Object> requestBody = Map.of(
                "model", modelName,
                "prompt", prompt,
                "stream", false,
                "format", "json"
        );

        return webClient.post()
                .uri("/api/generate")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(timeout * 2)) // Timeout maior para extração
                .map(response -> {
                    Object responseObj = response.get("response");
                    return responseObj != null ? responseObj.toString() : "{\"memorias\": []}";
                })
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

        Map<String, Object> requestBody = Map.of(
                "model", modelName,
                "prompt", prompt,
                "stream", false
        );

        return webClient.post()
                .uri("/api/generate")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(timeout))
                .map(response -> {
                    Object responseObj = response.get("response");
                    return responseObj != null ? responseObj.toString().trim() : "";
                })
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

        Map<String, Object> requestBody = Map.of(
                "model", modelName,
                "prompt", prompt,
                "stream", false,
                "format", "json"
        );

        return webClient.post()
                .uri("/api/generate")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(timeout))
                .map(response -> {
                    Object responseObj = response.get("response");
                    return responseObj != null ? responseObj.toString() : "{}";
                })
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

        Map<String, Object> requestBody = Map.of(
                "model", modelName,
                "prompt", prompt,
                "stream", false,
                "format", "json"
        );

        return webClient.post()
                .uri("/api/generate")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(timeout * 2))
                .map(response -> {
                    Object responseObj = response.get("response");
                    return responseObj != null ? responseObj.toString() : "{\"analiseEmocional\": {}, \"memorias\": [], \"gatilhos\": []}";
                })
                .onErrorReturn("{\"analiseEmocional\": {}, \"memorias\": [], \"gatilhos\": []}");
    }
}

