package br.jeanjacintho.tideflow.ai_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
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
}

