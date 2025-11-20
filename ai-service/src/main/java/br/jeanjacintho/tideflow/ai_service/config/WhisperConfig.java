package br.jeanjacintho.tideflow.ai_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WhisperConfig {

    @Value("${whisper.service.url:http://localhost:8001}")
    private String whisperServiceUrl;

    @Bean
    public WebClient whisperWebClient() {
        return WebClient.builder()
                .baseUrl(whisperServiceUrl)
                .build();
    }
}


