package com.portfolio.aiintro.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ╔══════════════════════════════════════════════════════╗
 * ║  CONCEITO: ChatMemory                                ║
 * ╠══════════════════════════════════════════════════════╣
 * ║  LLMs são STATELESS por natureza — cada chamada à   ║
 * ║  API é independente e o modelo não lembra de nada.  ║
 * ║                                                      ║
 * ║  Para simular memória, precisamos enviar o histórico ║
 * ║  completo da conversa a cada nova mensagem.          ║
 * ║                                                      ║
 * ║  O Spring AI faz isso automaticamente via            ║
 * ║  InMemoryChatMemory + MessageChatMemoryAdvisor.      ║
 * ║                                                      ║
 * ║  InMemoryChatMemory = HashMap<conversationId, List>  ║
 * ║  → armazena o histórico em memória RAM               ║
 * ║  → perdido ao reiniciar (ideal para estudos)         ║
 * ║  → próximo passo: persistir em banco de dados        ║
 * ╚══════════════════════════════════════════════════════╝
 */
@Configuration
public class ChatMemoryConfig {

    /**
     * Registra o InMemoryChatMemory como bean Spring.
     * Será injetado no ChatService automaticamente.
     *
     * Alternativas futuras:
     *  - CassandraChatMemory  → persistência em Cassandra
     *  - JdbcChatMemory       → persistência em banco relacional
     *  - RedisChatMemory      → persistência em Redis
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }
}
