package com.portfolio.aiintro.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.lang.management.MemoryManagerMXBean;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  MUDANÇAS neste branch vs feat/02-chat-memory                ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  ANTES: .call() → aguarda resposta completa (bloqueante)     ║
 * ║  AGORA: .stream() → devolve Flux<String> token a token       ║
 * ║                                                              ║
 * ║  O que é Streaming?                                          ║
 * ║  Em vez de esperar o modelo gerar TODA a resposta para       ║
 * ║  devolver, enviamos cada token (palavra/pedaço) assim que    ║
 * ║  fica pronto. O resultado é o efeito "digitando" do ChatGPT. ║
 * ║                                                              ║
 * ║  Tecnologia usada: Server-Sent Events (SSE)                  ║
 * ║  → protocolo HTTP unidirecional (servidor → cliente)         ║
 * ║  → suportado nativamente pelo browser via EventSource        ║
 * ║  → Spring usa MediaType.TEXT_EVENT_STREAM_VALUE              ║
 * ║                                                              ║
 * ║  Stack reativa: Spring WebFlux + Project Reactor             ║
 * ║  → Flux<T> = stream assíncrono de N elementos                ║
 * ║  → Mono<T> = stream assíncrono de 0 ou 1 elemento            ║
 * ╚══════════════════════════════════════════════════════════════╝
 **/
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
                //                                              Ajuste conforme o context window
                //                                              do seu modelo
                .call()
                .chatResponse();
    }

    /**
     * ╔══════════════════════════════════════════════════════╗
     * ║  NOVO: Chat com Streaming + Memória                  ║
     * ╚══════════════════════════════════════════════════════╝
     *
     * Retorna um Flux<String> onde cada elemento é um fragmento
     * de texto (token) emitido pelo modelo em tempo real.
     *
     * A única diferença para o chat normal:
     *   .call()   → ChatResponse (bloqueia até terminar)
     *   .stream() → Flux<ChatResponse> (emite a cada token)
     *
     * O .map() extrai apenas o texto de cada fragmento.
     * O .filter() remove tokens nulos que o modelo às vezes emite.
     *
     * @param userMessage    mensagem do usuário
     * @param conversationId chave da sessão para memória
     * @return stream de tokens em tempo real
     */

    public Flux<String> chatStream(String userMessage, String conversationId) {
        return chatClient.prompt()
                .user(userMessage)
                .advisors(
                        MessageChatMemoryAdvisor
                                .builder(chatMemory)
                                .conversationId(conversationId)
                                .build()
                ).stream()
                .chatResponse()
                .map(response -> response.getResult()
                        .getOutput()
                        .getText()
                ).filter( text -> text != null && !text.isEmpty());
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
