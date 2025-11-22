package br.jeanjacintho.tideflow.ai_service.service;

import br.jeanjacintho.tideflow.ai_service.client.LLMClient;
import br.jeanjacintho.tideflow.ai_service.dto.response.RiskAnalysisResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class RiskDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(RiskDetectionService.class);
    
    private final LLMClient llmClient;
    private final ObjectMapper objectMapper;
    
    private static final List<String> RISK_KEYWORDS = Arrays.asList(
        "quero me matar", "quero morrer", "vou me matar", "vou me suicidar",
        "quero me mutilar", "vou me cortar", "quero me machucar",
        "não aguento mais", "não vale a pena viver", "não quero mais viver",
        "vou acabar com tudo", "vou sumir", "ninguém vai sentir falta",
        "seria melhor se eu não existisse", "quero desaparecer"
    );

    public RiskDetectionService(LLMClient llmClient, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    public Mono<RiskAnalysisResponse> analyzeRisk(String message, String userId) {
        logger.info("Iniciando análise de risco para usuário {}: mensagem='{}'", userId, message);
        
        if (message == null || message.trim().isEmpty()) {
            logger.debug("Mensagem vazia, sem risco detectado");
            return Mono.just(new RiskAnalysisResponse(false, "NONE", "Mensagem vazia", null, 0.0));
        }
        
        String messageLower = message.toLowerCase();
        
        boolean hasRiskKeywords = RISK_KEYWORDS.stream()
                .anyMatch(keyword -> messageLower.contains(keyword.toLowerCase()));

        logger.debug("Verificação de palavras-chave: encontradas={}", hasRiskKeywords);
        
        if (!hasRiskKeywords) {
            logger.debug("Nenhuma palavra-chave de risco encontrada, mas ainda assim analisando com IA para detectar padrões sutis");
            // Mesmo sem palavras-chave explícitas, vamos analisar com IA para detectar padrões sutis
        }

        String analysisPrompt = buildRiskAnalysisPrompt(message);
        logger.debug("Prompt de análise construído, enviando para IA");
        
        return llmClient.generateResponse(analysisPrompt)
                .map(analysisResponse -> {
                    logger.debug("Resposta da IA recebida, fazendo parse");
                    RiskAnalysisResponse parsed = parseRiskAnalysisResponse(analysisResponse);
                    logger.info("Análise de risco parseada: detectado={}, nível={}, confiança={}", 
                        parsed.isRiskDetected(), parsed.getRiskLevel(), parsed.getConfidence());
                    return parsed;
                })
                .onErrorResume(error -> {
                    logger.error("Erro ao analisar risco com IA para usuário {}: {}", userId, error.getMessage(), error);
                    RiskAnalysisResponse fallback = createFallbackRiskAnalysis(message);
                    logger.info("Usando análise de fallback: detectado={}, nível={}", 
                        fallback.isRiskDetected(), fallback.getRiskLevel());
                    return Mono.just(fallback);
                });
    }

    private String buildRiskAnalysisPrompt(String message) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analise a seguinte mensagem de um usuário e determine se há risco de autolesão ou suicídio.\n\n");
        prompt.append("Mensagem: ").append(message).append("\n\n");
        prompt.append("Considere o contexto completo da mensagem. Palavras como 'quero me matar' podem ser usadas de forma figurativa ou em contexto de ficção/arte.\n\n");
        prompt.append("Responda APENAS com um JSON válido no seguinte formato:\n");
        prompt.append("{\n");
        prompt.append("  \"isRiskDetected\": true/false,\n");
        prompt.append("  \"riskLevel\": \"LOW\" | \"MEDIUM\" | \"HIGH\" | \"CRITICAL\" | \"NONE\",\n");
        prompt.append("  \"reason\": \"Explicação breve do motivo da detecção ou não detecção\",\n");
        prompt.append("  \"context\": \"Contexto da mensagem que ajudou na análise\",\n");
        prompt.append("  \"confidence\": 0.0-1.0\n");
        prompt.append("}\n\n");
        prompt.append("Critérios:\n");
        prompt.append("- Se a mensagem claramente indica intenção de autolesão ou suicídio no contexto real, marque como risco.\n");
        prompt.append("- Se for uso figurativo, metafórico, ou em contexto de ficção/arte, NÃO marque como risco.\n");
        prompt.append("- Se houver ambiguidade, seja conservador e marque como risco baixo/médio.\n");
        prompt.append("- Confidence deve refletir sua certeza na análise (0.0 = incerto, 1.0 = muito certo).\n");
        
        return prompt.toString();
    }

    private RiskAnalysisResponse parseRiskAnalysisResponse(String jsonResponse) {
        try {
            String cleanedResponse = jsonResponse
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();
            
            Map<String, Object> responseMap = objectMapper.readValue(
                    cleanedResponse, 
                    new TypeReference<Map<String, Object>>() {}
            );
            
            boolean isRiskDetected = Boolean.TRUE.equals(responseMap.get("isRiskDetected"));
            String riskLevel = (String) responseMap.getOrDefault("riskLevel", "NONE");
            String reason = (String) responseMap.getOrDefault("reason", "");
            String context = (String) responseMap.getOrDefault("context", "");
            
            double confidence = 0.5;
            Object confidenceObj = responseMap.get("confidence");
            if (confidenceObj instanceof Number) {
                confidence = ((Number) confidenceObj).doubleValue();
            } else if (confidenceObj instanceof String) {
                try {
                    confidence = Double.parseDouble((String) confidenceObj);
                } catch (NumberFormatException e) {
                    logger.warn("Erro ao parsear confidence: {}", confidenceObj);
                }
            }
            
            confidence = Math.max(0.0, Math.min(1.0, confidence));
            
            if (!isRiskDetected) {
                riskLevel = "NONE";
            }
            
            return new RiskAnalysisResponse(isRiskDetected, riskLevel, reason, context, confidence);
            
        } catch (JsonProcessingException e) {
            logger.error("Erro ao fazer parse da resposta de análise de risco: {}", e.getMessage(), e);
            return createFallbackRiskAnalysis(null);
        }
    }

    private RiskAnalysisResponse createFallbackRiskAnalysis(String message) {
        if (message == null) {
            return new RiskAnalysisResponse(false, "NONE", "Erro na análise", null, 0.0);
        }
        
        String messageLower = message.toLowerCase();
        boolean hasHighRiskKeywords = messageLower.contains("quero me matar") || 
                                      messageLower.contains("vou me matar") ||
                                      messageLower.contains("quero me suicidar");
        
        if (hasHighRiskKeywords) {
            return new RiskAnalysisResponse(true, "HIGH", 
                    "Detecção de palavras-chave de alto risco", message, 0.7);
        }
        
        return new RiskAnalysisResponse(false, "NONE", 
                "Análise não concluída - análise manual recomendada", null, 0.3);
    }
}
