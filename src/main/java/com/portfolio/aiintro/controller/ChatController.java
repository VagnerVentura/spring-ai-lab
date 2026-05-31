package com.portfolio.aiintro.controller;

import com.portfolio.aiintro.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ChatController — expõe os endpoints REST da aplicação.
 *
 * Endpoints disponíveis:
 *   GET  /api/chat/hello        → teste rápido sem parâmetros
 *   GET  /api/chat?message=...  → pergunta via query param (fácil de testar no browser)
 *   POST /api/chat              → pergunta via body JSON (uso real)
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * GET /api/chat/hello
     * Endpoint de aquecimento — bom para testar se a API key está funcionando.
     * Acesse: http://localhost:8080/api/chat/hello
     */
    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        String response = chatService.chatSimple(
                "Diga 'Olá! Sou o assistente Java, pronto para ajudar.' e mais nada."
        );
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/chat?message=como funciona um record em java
     * Forma rápida de testar direto no browser ou no curl.
     * Exemplo: http://localhost:8080/api/chat?message=o que é Spring AI?
     */
    @GetMapping
    public ResponseEntity<String> chatGet(@RequestParam String message) {
        String response = chatService.chatSimple(message);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/chat
     * Endpoint principal — recebe JSON e devolve JSON com metadados.
     *
     * Corpo da requisição:
     * {
     *   "message": "O que é um context window?"
     * }
     *
     * Resposta:
     * {
     *   "response": "...",
     *   "model": "claude-3-5-haiku-...",
     *   "tokensUsed": 123
     * }
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chatPost(@RequestBody ChatRequest request) {

        var aiResponse = chatService.chat(request.message());

        // Extraindo dados do objeto de resposta do Spring AI
        String text       = aiResponse.getResult().getOutput().getText();
        String model      = aiResponse.getMetadata().getModel();
        long tokensUsed   = aiResponse.getMetadata().getUsage().getTotalTokens();

        return ResponseEntity.ok(new ChatResponse(text, model, tokensUsed));
    }
}
