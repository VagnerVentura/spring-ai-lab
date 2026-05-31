package com.portfolio.aiintro.service;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ChatService — camada de serviço responsável por montar o prompt
 * e chamar o modelo via Spring AI.
 *
 * Conceitos que você vai aprender aqui:
 *  - SystemMessage: instrução de contexto/persona para o modelo
 *  - UserMessage: mensagem do usuário
 *  - Prompt: agrupa as mensagens antes de enviar
 *  - ChatResponse: resposta completa com metadados (tokens, model, etc.)
 */
@Service
public class ChatService {

    private final OllamaChatModel chatModel;

    // Spring injeta o modelo configurado no application.properties
    public ChatService(OllamaChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Envia uma mensagem simples ao modelo e retorna a resposta.
     *
     * @param userMessage mensagem enviada pelo usuário
     * @return resposta completa do modelo (ChatResponse do Spring AI)
     */
    public ChatResponse chat(String userMessage) {

        // System prompt: define o comportamento e persona do assistente
        // Experimente mudar esse texto e veja como o modelo responde diferente!
        var systemMessage = new SystemMessage("""
                Você é um assistente especializado em desenvolvimento Java e Spring Boot.
                Responda de forma clara e didática, com exemplos de código quando relevante.
                Sempre responda em português brasileiro.
                """);

        // User message: o que o usuário enviou
        var userMsg = new UserMessage(userMessage);

        // Prompt: agrupa todas as mensagens em ordem
        var prompt = new Prompt(List.of(systemMessage, userMsg));

        // Chama o modelo e retorna o objeto de resposta completo
        return chatModel.call(prompt);
    }

    /**
     * Versão simplificada que retorna apenas o texto da resposta.
     * Útil para casos onde não precisamos dos metadados.
     */
    public String chatSimple(String userMessage) {
        return chatModel.call(userMessage);
    }
}
