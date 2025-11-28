package br.jeanjacintho.tideflow.ai_service.client;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface LLMClient {

    Mono<String> generateResponse(String prompt);

    Mono<String> chatWithHistory(List<Map<String, String>> messages);

    Mono<String> extractMemories(String userMessage, String aiResponse);

    Mono<String> generateProactiveQuestion(String memoriaConteudo, String memoriaTipo);

    Mono<String> extractEmotionalAnalysis(String userMessage);

    Mono<String> extractEmotionalAnalysisAndMemories(String userMessage, String aiResponse);
}
