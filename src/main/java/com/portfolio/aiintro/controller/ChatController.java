package com.portfolio.aiintro.controller;

import com.portfolio.aiintro.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;

/**
 * ChatController — adicionado endpoint SSE para streaming.
 *
 * Endpoints:
 *   GET  /api/chat/hello                    → teste rápido
 *   GET  /api/chat?message=...              → resposta simples
 *   POST /api/chat                          → resposta completa + memória (JSON)
 *   POST /api/chat/stream?cid=...           → streaming SSE + memória  ← NOVO
 *   DELETE /api/chat/{conversationId}       → limpa histórico
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok(chatService.chatSimple(
                "Diga 'Olá! Sou o assistente Java com streaming ativo.' e mais nada."
        ));
    }

    @GetMapping
    public ResponseEntity<String> chatGet(@RequestParam String message) {
        return ResponseEntity.ok(chatService.chatSimple(message));
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chatPost(@RequestBody ChatRequest request) {
        String convId = resolveConversationId(request.conversationId());
        var aiResponse = chatService.chat(request.message(), convId);

        String text  = aiResponse.getResult().getOutput().getText();
        String model = aiResponse.getMetadata().getModel();
        long tokens  = aiResponse.getMetadata().getUsage().getTotalTokens();

        return ResponseEntity.ok(new ChatResponse(text, model, tokens, convId));
    }

    /**
     * ╔══════════════════════════════════════════════════════════╗
     * ║  NOVO: POST /api/chat/stream                             ║
     * ╠══════════════════════════════════════════════════════════╣
     * ║  Retorna Server-Sent Events (SSE).                       ║
     * ║                                                          ║
     * ║  MediaType.TEXT_EVENT_STREAM_VALUE = "text/event-stream" ║
     * ║  → formato: "data: token\n\n"                           ║
     * ║  → o browser processa cada evento conforme chega         ║
     * ║                                                          ║
     * ║  No frontend, consuma com:                               ║
     * ║    const es = new EventSource('/api/chat/stream?...')    ║
     * ║    es.onmessage = e => render(e.data)                    ║
     * ║                                                          ║
     * ║  Por que POST com @RequestBody não funciona com SSE?     ║
     * ║  EventSource do browser só suporta GET. Para POST+SSE,   ║
     * ║  usamos fetch() com ReadableStream no frontend.          ║
     * ║  Aqui usamos query params para simplificar.              ║
     * ╚══════════════════════════════════════════════════════════╝
     *
     * Exemplo curl (veja os tokens chegando em tempo real!):
      curl -N -X POST "http://localhost:8080/api/chat/stream" `
        -H "Content-Type: application/json" `
        -d '{"message":"explique streams em Java","conversationId":"s1"}'
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        String convId = resolveConversationId(request.conversationId());
        return chatService.chatStream(request.message(), convId);
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Map<String, String>> clearMemory(@PathVariable String conversationId) {
        chatService.clearMemory(conversationId);
        return ResponseEntity.ok(Map.of("status", "cleared", "conversationId", conversationId));
    }

    private String resolveConversationId(String id) {
        return (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
    }
}
