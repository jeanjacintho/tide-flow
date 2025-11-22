package br.jeanjacintho.tideflow.ai_service.client;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Interface para clientes de LLM (Large Language Models).
 * Permite alternar entre diferentes provedores (Ollama, Gemini, etc.)
 */
public interface LLMClient {
    
    /**
     * Gera uma resposta baseada em um prompt simples.
     */
    Mono<String> generateResponse(String prompt);
    
    /**
     * Chat com histórico de mensagens.
     */
    Mono<String> chatWithHistory(List<Map<String, String>> messages);
    
    /**
     * Extrai memórias e gatilhos de uma conversa.
     */
    Mono<String> extractMemories(String userMessage, String aiResponse);
    
    /**
     * Gera uma pergunta proativa baseada em uma memória.
     */
    Mono<String> generateProactiveQuestion(String memoriaConteudo, String memoriaTipo);
    
    /**
     * Extrai análise emocional de uma mensagem do usuário.
     */
    Mono<String> extractEmotionalAnalysis(String userMessage);
    
    /**
     * Extrai análise emocional e memórias em uma única requisição.
     * Retorna JSON com ambos os dados para otimizar chamadas à API.
     */
    Mono<String> extractEmotionalAnalysisAndMemories(String userMessage, String aiResponse);
}

