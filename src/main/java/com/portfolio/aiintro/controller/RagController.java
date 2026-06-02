package com.portfolio.aiintro.controller;

import com.portfolio.aiintro.rag.DocumentIngestionService;
import com.portfolio.aiintro.rag.RagChatService;
import org.springframework.ai.document.Document;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RagController — endpoints exclusivos do pipeline RAG.
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │  POST   /api/rag/ingest            Upload e indexa arquivo  │
 * │  POST   /api/rag/ask               Pergunta com RAG (JSON)  │
 * │  POST   /api/rag/ask/stream        Pergunta com RAG (SSE)   │
 * │  GET    /api/rag/search?q=...      Busca vetorial pura      │
 * └─────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final DocumentIngestionService ingestionService;
    private final RagChatService           ragChatService;

    public RagController(DocumentIngestionService ingestionService,
                         RagChatService ragChatService) {
        this.ingestionService = ingestionService;
        this.ragChatService   = ragChatService;
    }

    /**
     * POST /api/rag/ingest
     *
     * Recebe um arquivo e dispara o pipeline de ingestão:
     * Load → Split → Embed → Store no pgvector.
     *
     * Exemplo com curl:
     * curl -X POST http://localhost:8080/api/rag/ingest \
     *   -F "file=@/caminho/para/documento.pdf" \
     *   -F "source=manual-spring-ai"
     *
     * Formatos suportados: PDF, TXT, DOCX, HTML, Markdown, ODT, EPUB...
     */
    @PostMapping(value = "/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> ingest(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "source", defaultValue = "documento") String source
    ) {
        try {
            int chunks = ingestionService.ingest(file, source);
            return ResponseEntity.ok(Map.of(
                    "status",    "indexed",
                    "source",    source,
                    "filename",  file.getOriginalFilename(),
                    "chunks",    chunks,
                    "sizeBytes", file.getSize()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/rag/ask
     *
     * Faz uma pergunta sobre os documentos indexados.
     * Retorna resposta completa em JSON + metadados.
     *
     * Exemplo:
     * curl -X POST http://localhost:8080/api/rag/ask \
     *   -H "Content-Type: application/json" \
     *   -d '{"message": "O que é o QuestionAnswerAdvisor?", "conversationId": "s1"}'
     */
    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> ask(@RequestBody ChatRequest request) {
        String convId = resolveId(request.conversationId());

        var aiResponse = ragChatService.ask(request.message(), convId);

        String text   = aiResponse.getResult().getOutput().getText();
        String model  = aiResponse.getMetadata().getModel();
        long tokens   = aiResponse.getMetadata().getUsage().getTotalTokens();

        return ResponseEntity.ok(new ChatResponse(text, model, tokens, convId));
    }

    /**
     * POST /api/rag/ask/stream
     *
     * Mesma pergunta RAG, mas com streaming SSE.
     * Resposta aparece token a token no frontend.
     *
     * curl -N -X POST http://localhost:8080/api/rag/ask/stream \
     *   -H "Content-Type: application/json" \
     *   -d '{"message": "resuma o documento", "conversationId": "s1"}'
     */
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> askStream(@RequestBody ChatRequest request) {
        String convId = resolveId(request.conversationId());
        return ragChatService.askStream(request.message(), convId);
    }

    /**
     * GET /api/rag/search?q=texto+da+busca
     *
     * Busca vetorial PURA — sem LLM, retorna os chunks brutos.
     * Use para depurar o pipeline:
     *   → Os chunks certos estão sendo encontrados?
     *   → O threshold de similaridade está adequado?
     *   → Os metadados estão corretos?
     *
     * Exemplo:
     * curl "http://localhost:8080/api/rag/search?q=como+funciona+o+RAG"
     */
    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> search(@RequestParam("q") String query) {
        List<Document> docs = ragChatService.search(query);

        // Mapeia para um formato legível — conteúdo + metadados
        var result = docs.stream()
                .map(doc -> Map.<String, Object>of(
                        "content",  doc.getText(),
                        "metadata", doc.getMetadata(),
                        "score",    doc.getMetadata().getOrDefault("distance", "n/a")
                ))
                .toList();

        return ResponseEntity.ok(result);
    }

    private String resolveId(String id) {
        return (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
    }
}
