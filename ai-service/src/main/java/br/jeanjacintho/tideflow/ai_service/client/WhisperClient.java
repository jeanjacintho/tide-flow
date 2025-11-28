package br.jeanjacintho.tideflow.ai_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Component
public class WhisperClient {

    private final WebClient webClient;
    private final int timeout;
    private final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();

    public WhisperClient(WebClient whisperWebClient,
                        @Value("${timeout:60000}") int timeout) {
        this.webClient = whisperWebClient;
        this.timeout = timeout;
    }

    public Mono<String> transcribeAudio(byte[] audioData, String filename) {
        DataBuffer dataBuffer = bufferFactory.wrap(audioData);

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("audio", dataBuffer)
                .filename(filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM);

        return webClient.post()
                .uri("/transcribe")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(timeout * 3))
                .map(response -> {
                    Object textObj = response.get("text");
                    return textObj != null ? textObj.toString().trim() : "";
                })
                .onErrorReturn("Erro ao transcrever o áudio.");
    }
}
