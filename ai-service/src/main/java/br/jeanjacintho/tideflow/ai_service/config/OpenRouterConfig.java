package br.jeanjacintho.tideflow.ai_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuração para o cliente OpenRouter.
 * Só é criado se a propriedade llm.provider estiver configurada como "openrouter".
 */
@Configuration
@ConditionalOnProperty(name = "llm.provider", havingValue = "openrouter")
public class OpenRouterConfig {

    @Value("${openrouter.base.url:https://openrouter.ai}")
    private String openRouterBaseUrl;

    @Bean
    public WebClient openRouterWebClient() {
        return WebClient.builder()
                .baseUrl(openRouterBaseUrl)
                .build();
    }
}

