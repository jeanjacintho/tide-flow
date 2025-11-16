package br.jeanjacintho.tideflow.ai_service.service;

import br.jeanjacintho.tideflow.ai_service.client.OllamaClient;
import br.jeanjacintho.tideflow.ai_service.dto.request.ConversationRequest;
import br.jeanjacintho.tideflow.ai_service.dto.response.ConversationResponse;
import br.jeanjacintho.tideflow.ai_service.model.EmotionalAnalysis;
import com.felipestanzani.jtoon.JToon;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class ConversationService {

    private final OllamaClient ollamaClient;
    private final Map<String, List<Map<String, String>>> conversationHistory = new HashMap<>();

    public ConversationService(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    public Mono<ConversationResponse> processConversation(ConversationRequest request) {
        String conversationId = request.getConverationId() != null 
                ? request.getConverationId() 
                : UUID.randomUUID().toString();

        List<Map<String, String>> history = conversationHistory.getOrDefault(conversationId, new ArrayList<>());
        
        Map<String, String> userMessage = Map.of("role", "user", "content", request.getMessage());
        history.add(userMessage);

        String toonPrompt = buildToonPrompt(history, request.getUserId());
        
        return ollamaClient.generateResponse(toonPrompt)
                .map(aiResponse -> {
                    Map<String, String> assistantMessage = Map.of("role", "assistant", "content", aiResponse);
                    history.add(assistantMessage);
                    conversationHistory.put(conversationId, history);

                    EmotionalAnalysis analysis = extractEmotionalAnalysis(aiResponse);
                    
                    return new ConversationResponse(
                            aiResponse,
                            conversationId,
                            false,
                            analysis
                    );
                });
    }

    private String buildToonPrompt(List<Map<String, String>> history, String userId) {
        Map<String, Object> conversationData = new LinkedHashMap<>();
        conversationData.put("userId", userId);
        conversationData.put("role", "terapeuta");
        conversationData.put("objetivo", "entender emoções e fazer perguntas empáticas");
        
        List<Map<String, Object>> messages = new ArrayList<>();
        for (Map<String, String> msg : history) {
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("role", msg.get("role"));
            message.put("text", msg.get("content"));
            messages.add(message);
        }
        conversationData.put("messages", messages);

        String toon = JToon.encode(conversationData);
        
        return "Conversa em TOON:\n" + toon + 
               "\nResponda como terapeuta empática, fazendo perguntas para entender as emoções.";
    }

    private EmotionalAnalysis extractEmotionalAnalysis(String aiResponse) {
        String lowerResponse = aiResponse.toLowerCase();
        
        String primaryEmotional = "neutro";
        if (lowerResponse.contains("triste") || lowerResponse.contains("tristeza")) {
            primaryEmotional = "tristeza";
        } else if (lowerResponse.contains("ansioso") || lowerResponse.contains("ansiedade")) {
            primaryEmotional = "ansiedade";
        } else if (lowerResponse.contains("feliz") || lowerResponse.contains("alegria")) {
            primaryEmotional = "alegria";
        } else if (lowerResponse.contains("raiva") || lowerResponse.contains("irritado")) {
            primaryEmotional = "raiva";
        }

        int intensity = 5;
        if (lowerResponse.contains("muito") || lowerResponse.contains("extremamente")) {
            intensity = 8;
        } else if (lowerResponse.contains("pouco") || lowerResponse.contains("levemente")) {
            intensity = 3;
        }

        List<String> triggers = new ArrayList<>();
        if (lowerResponse.contains("trabalho")) triggers.add("trabalho");
        if (lowerResponse.contains("relacionamento")) triggers.add("relacionamento");
        if (lowerResponse.contains("família")) triggers.add("família");

        return new EmotionalAnalysis(
                primaryEmotional,
                intensity,
                triggers,
                aiResponse.substring(0, Math.min(200, aiResponse.length())),
                "Continue conversando para entender melhor suas emoções."
        );
    }
}

