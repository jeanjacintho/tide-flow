package br.jeanjacintho.tideflow.ai_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class OllamaClient {

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
            "  ]\n" +
            "}\n\n" +
            "Se não houver informações importantes para lembrar, retorne: {\"memorias\": []}",
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
}

