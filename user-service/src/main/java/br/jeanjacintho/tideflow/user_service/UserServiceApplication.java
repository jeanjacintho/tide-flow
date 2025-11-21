package br.jeanjacintho.tideflow.user_service;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UserServiceApplication {

	private static final Logger logger = LoggerFactory.getLogger(UserServiceApplication.class);

	public static void main(String[] args) {
		loadEnvFile();
		SpringApplication.run(UserServiceApplication.class, args);
	}

	private static void loadEnvFile() {
		try {
			Dotenv dotenv = null;
			java.io.File envFile = new java.io.File("../.env");
			
			if (envFile.exists()) {
				dotenv = Dotenv.configure().directory("../").filename(".env").ignoreIfMissing().load();
			} else {
				envFile = new java.io.File(".env");
				if (envFile.exists()) {
					dotenv = Dotenv.configure().directory(".").filename(".env").ignoreIfMissing().load();
				} else {
					return;
				}
			}

			if (dotenv == null || dotenv.entries().isEmpty()) {
				return;
			}

			dotenv.entries().forEach(entry -> {
				if (System.getProperty(entry.getKey()) == null) {
					System.setProperty(entry.getKey(), entry.getValue());
				}
			});
			
			logger.info("Carregadas {} variáveis do .env", dotenv.entries().size());
		} catch (Exception e) {
			logger.debug("Arquivo .env não encontrado ou erro ao carregar: {}", e.getMessage());
		}
	}

}
