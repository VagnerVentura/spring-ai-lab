package com.portfolio.aiintro.controller;

import com.portfolio.aiintro.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * ChatController — atualizado com suporte a memória de conversa.
 *
 * Endpoints:
 *   GET  /api/chat/hello                    → teste rápido
 *   GET  /api/chat?message=...              → sem memória (stateless)
 *   POST /api/chat                          → COM memória (conversationId)
 *   DELETE /api/chat/{conversationId}       → limpa histórico de uma sessão
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
                "Diga 'Olá! Sou o assistente Java com memória de conversa, pronto para ajudar.' e mais nada."
        ));
    }

    @GetMapping
    public ResponseEntity<String> chatGet(@RequestParam String message) {
        return ResponseEntity.ok(chatService.chatSimple(message));
    }

    /**
     * POST /api/chat
     *
     * Se conversationId não for enviado, geramos um UUID automaticamente.
     * O cliente deve salvar o conversationId retornado e reusá-lo
     * nas próximas mensagens da mesma sessão.
     *
     * Exemplo de fluxo:
     *
     * Req 1: { "message": "meu nome é Vagner", "conversationId": null }
     * Res 1: { ..., "conversationId": "abc-123" }
     *
     * Req 2: { "message": "qual é o meu nome?", "conversationId": "abc-123" }
     * Res 2: { "response": "Seu nome é Vagner!", ... }
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chatPost(@RequestBody ChatRequest request) {

        // Garante que sempre haverá um conversationId
        String convId = (request.conversationId() != null && !request.conversationId().isBlank())
                ? request.conversationId()
                : UUID.randomUUID().toString();

        var aiResponse = chatService.chat(request.message(), convId);

        String text      = aiResponse.getResult().getOutput().getText();
        String model     = aiResponse.getMetadata().getModel();
        long tokens      = aiResponse.getMetadata().getUsage().getTotalTokens();

        return ResponseEntity.ok(new ChatResponse(text, model, tokens, convId));
    }

    /**
     * DELETE /api/chat/{conversationId}
     *
     * Limpa o histórico de uma conversa.
     * Equivalente a um "Nova conversa" no frontend.
     *
     * Exemplo:
     * curl -X DELETE http://localhost:8080/api/chat/abc-123
     */
    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Map<String, String>> clearMemory(@PathVariable String conversationId) {
        chatService.clearMemory(conversationId);
        return ResponseEntity.ok(Map.of(
                "status", "cleared",
                "conversationId", conversationId
        ));
    }
}
