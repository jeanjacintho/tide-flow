package br.jeanjacintho.tideflow.ai_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@ConditionalOnProperty(name = "llm.provider", havingValue = "gemini")
public class GeminiConfig {

    @Value("${gemini.base.url:https://generativelanguage.googleapis.com}")
    private String geminiBaseUrl;

    @Bean
    public WebClient geminiWebClient() {
        return WebClient.builder()
                .baseUrl(geminiBaseUrl)
                .build();
    }
}
