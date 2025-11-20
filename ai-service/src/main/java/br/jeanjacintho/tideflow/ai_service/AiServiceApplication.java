package br.jeanjacintho.tideflow.ai_service;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableCaching
public class AiServiceApplication {

	private static final Logger logger = LoggerFactory.getLogger(AiServiceApplication.class);

	public static void main(String[] args) {
		// Carrega variáveis do arquivo .env se existir
		loadEnvFile();
		
		SpringApplication.run(AiServiceApplication.class, args);
	}

	private static void loadEnvFile() {
		try {
			Dotenv dotenv = Dotenv.configure()
					.directory(".")
					.directory("../")
					.ignoreIfMissing()
					.load();

			// Carrega variáveis do .env como System Properties (Spring Boot lê via ${VAR_NAME:})
			dotenv.entries().forEach(entry -> {
				if (System.getenv(entry.getKey()) == null) {
					System.setProperty(entry.getKey(), entry.getValue());
				}
			});
			
			if (dotenv.entries().size() > 0) {
				logger.info("Carregadas {} variáveis do .env", dotenv.entries().size());
			}
		} catch (Exception e) {
			logger.debug("Arquivo .env não encontrado ou erro ao carregar: {}", e.getMessage());
		}
	}

}
