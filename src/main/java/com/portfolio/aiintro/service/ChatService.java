package com.portfolio.aiintro.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

import java.lang.management.MemoryManagerMXBean;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  MUDANÇAS neste branch vs feat/01-chat-basico                ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  ANTES: usávamos OllamaChatModel diretamente                 ║
 * ║  AGORA: usamos ChatClient — API fluente do Spring AI         ║
 * ║                                                              ║
 * ║  ChatClient é o "builder" do Spring AI. Permite encadear:    ║
 * ║  - system()     → system prompt                              ║
 * ║  - user()       → mensagem do usuário                        ║
 * ║  - advisors()   → middlewares (memória, logging, RAG...)     ║
 * ║  - call()       → executa e retorna resposta                 ║
 * ║                                                              ║
 * ║  MessageChatMemoryAdvisor é o "middleware de memória":       ║
 * ║  → intercepta cada chamada                                   ║
 * ║  → injeta o histórico da conversa no prompt                  ║
 * ║  → salva a nova mensagem + resposta na memória               ║
 * ║                                                              ║
 * ║  Tudo isso transparente — você só passa o conversationId!    ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
@Service
public class ChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    /**
     * ChatClient.Builder é auto-configurado pelo Spring AI.
     * Basta injetá-lo e chamar .build().
     */
    public ChatService(ChatClient.Builder builder, ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
        this.chatClient = builder
                .defaultSystem("""
                        Você é um assistente especializado em desenvolvimento Java e Spring Boot.
                        Responda de forma clara e didática, com exemplos de código quando relevante.
                        Sempre responda em português brasileiro.
                        Lembre-se do contexto da conversa para dar respostas coerentes.
                        """)
                .build();
    }

    /**
     * Chat com memória de conversa.
     *
     * O MessageChatMemoryAdvisor faz toda a mágica:
     * 1. Busca o histórico de `conversationId` no ChatMemory
     * 2. Injeta as mensagens anteriores no prompt
     * 3. Envia tudo para o modelo
     * 4. Salva a nova troca no histórico
     *
     * @param userMessage    o que o usuário enviou agora
     * @param conversationId chave da sessão (use UUID.randomUUID() no cliente)
     */
    public ChatResponse chat(String userMessage, String conversationId) {
        return chatClient.prompt()
                .user(userMessage)
                .advisors(
                        MessageChatMemoryAdvisor
                        .builder(chatMemory)
                        .conversationId(conversationId)
                        .build()
                )
                //                                                              ↑
                //                                              windowSize=20: mantém as
                //                                              últimas 20 mensagens no contexto
                //                                              Ajuste conforme o context window
                //                                              do seu modelo
                .call()
                .chatResponse();
    }

    /**
     * Versão simples sem memória — mantida para o endpoint GET /hello.
     */
    public String chatSimple(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }

    /**
     * Limpa o histórico de uma conversa específica.
     * Útil para implementar um botão "Nova conversa" no frontend.
     */
    public void clearMemory(String conversationId) {
        chatMemory.clear(conversationId);
    }
}
