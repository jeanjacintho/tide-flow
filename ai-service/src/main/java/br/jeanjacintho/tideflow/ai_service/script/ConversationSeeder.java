package br.jeanjacintho.tideflow.ai_service.script;

import br.jeanjacintho.tideflow.ai_service.model.Conversation;
import br.jeanjacintho.tideflow.ai_service.model.ConversationMessage;
import br.jeanjacintho.tideflow.ai_service.model.MessageRole;
import br.jeanjacintho.tideflow.ai_service.repository.ConversationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.UUID;

@Component
public class ConversationSeeder implements CommandLineRunner {

    @Autowired
    private ConversationRepository conversationRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (args.length > 0 && "seed".equals(args[0])) {
            System.out.println("💬 Iniciando população de conversas...");
            
            seedConversations();
            
            System.out.println("✅ Conversas populadas com sucesso!");
        }
    }

    private void seedConversations() {
        List<String> userIds = getEmployeeUserIds();
        
        if (userIds.isEmpty()) {
            System.out.println("⚠️  Nenhum usuário encontrado. Execute primeiro o seed do user-service.");
            return;
        }

        Random random = new Random();
        int totalConversations = 0;

        for (String userId : userIds) {
            int conversationsPerUser = 2 + random.nextInt(4);
            
            for (int i = 0; i < conversationsPerUser; i++) {
                Conversation conversation = createConversationWithMessages(userId, random);
                conversationRepository.save(conversation);
                totalConversations++;
            }
        }

        System.out.println("✅ " + totalConversations + " conversas criadas para " + userIds.size() + " usuários");
    }

    private List<String> getEmployeeUserIds() {
        List<String> userIds = new ArrayList<>();
        
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            
            UUID companyId = getMoredevsCompanyId();
            if (companyId == null) {
                return userIds;
            }
            
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:8080/api/companies/" + companyId + "/users"))
                .header("Content-Type", "application/json")
                .GET()
                .build();
            
            java.net.http.HttpResponse<String> response = client.send(request, 
                java.net.http.HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode users = mapper.readTree(response.body());
                
                for (com.fasterxml.jackson.databind.JsonNode user : users) {
                    String email = user.get("email").asText();
                    if (email != null && email.contains("@moredevs.com")) {
                        userIds.add(user.get("id").asText());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️  Erro ao buscar usuários via API: " + e.getMessage());
        }
        
        return userIds;
    }

    private UUID getMoredevsCompanyId() {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:8080/api/companies"))
                .header("Content-Type", "application/json")
                .GET()
                .build();
            
            java.net.http.HttpResponse<String> response = client.send(request, 
                java.net.http.HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode companies = mapper.readTree(response.body());
                
                for (com.fasterxml.jackson.databind.JsonNode company : companies) {
                    if ("moredevs".equalsIgnoreCase(company.get("name").asText())) {
                        return UUID.fromString(company.get("id").asText());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️  Erro ao buscar companyId: " + e.getMessage());
        }
        
        return null;
    }

    private Conversation createConversationWithMessages(String userId, Random random) {
        Conversation conversation = new Conversation(userId);
        
        LocalDateTime baseTime = LocalDateTime.now().minusDays(random.nextInt(30));
        int messageCount = 4 + random.nextInt(8);
        
        String[][] conversationTemplates = {
            {
                "Olá, estou me sentindo muito estressado com o trabalho ultimamente.",
                "Entendo que você está passando por um momento difícil. Pode me contar mais sobre o que está causando esse estresse?",
                "É a pressão dos prazos e a sobrecarga de tarefas. Sinto que não consigo dar conta de tudo.",
                "É compreensível sentir-se sobrecarregado. Vamos trabalhar juntos para encontrar estratégias de gerenciamento de tempo e priorização que possam ajudar.",
                "Obrigado, isso me ajuda muito."
            },
            {
                "Tenho dificuldade para dormir pensando nos problemas do trabalho.",
                "Problemas de sono relacionados ao trabalho são comuns. Você já tentou alguma técnica de relaxamento antes de dormir?",
                "Não, nunca tentei. O que você sugere?",
                "Recomendo técnicas de respiração profunda, meditação ou até mesmo escrever suas preocupações em um diário antes de dormir. Isso pode ajudar a 'esvaziar' a mente.",
                "Vou tentar isso hoje à noite. Obrigado pela dica!"
            },
            {
                "Sinto que minha equipe não me valoriza.",
                "É difícil quando não nos sentimos reconhecidos. Você já conversou com sua equipe sobre como se sente?",
                "Não, tenho medo de parecer fraco ou reclamão.",
                "Expressar seus sentimentos não é sinal de fraqueza. Uma comunicação aberta e honesta pode melhorar muito os relacionamentos no trabalho.",
                "Você tem razão. Vou tentar ser mais aberto."
            },
            {
                "Estou pensando em pedir demissão, mas tenho medo.",
                "Essa é uma decisão importante. O que está te fazendo considerar essa possibilidade?",
                "A cultura da empresa não está alinhada com meus valores, e sinto que não tenho espaço para crescer.",
                "Antes de tomar uma decisão definitiva, considere se há possibilidade de mudança dentro da empresa ou se realmente é hora de buscar novas oportunidades.",
                "Vou refletir sobre isso. Obrigado pelo conselho."
            },
            {
                "Meu chefe está sempre me criticando e isso está afetando minha autoestima.",
                "Críticas constantes podem ser muito desgastantes. Você já tentou ter uma conversa franca com seu chefe sobre feedback construtivo?",
                "Não, tenho medo de piorar a situação.",
                "É importante estabelecer limites e buscar feedback construtivo. Se a situação não melhorar, considere buscar apoio do RH.",
                "Vou pensar nisso. Obrigado por me ouvir."
            }
        };

        String[] selectedTemplate = conversationTemplates[random.nextInt(conversationTemplates.length)];
        
        for (int i = 0; i < messageCount && i < selectedTemplate.length; i++) {
            MessageRole role = (i % 2 == 0) ? MessageRole.USER : MessageRole.ASSISTANT;
            String content = selectedTemplate[i];
            
            ConversationMessage message = new ConversationMessage();
            message.setRole(role);
            message.setContent(content);
            message.setSequenceNumber(i + 1);
            message.setCreatedAt(baseTime.plusMinutes(i * 5));
            message.setConversation(conversation);
            
            conversation.addMessage(message);
        }

        conversation.setCreatedAt(baseTime);
        conversation.setUpdatedAt(baseTime.plusMinutes(messageCount * 5));

        return conversation;
    }
}
