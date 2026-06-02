package com.portfolio.aiintro.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║  CONCEITO: Ingestão de Documentos (Indexing Pipeline)           ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║                                                                  ║
 * ║  O pipeline RAG tem duas fases:                                  ║
 * ║                                                                  ║
 * ║  FASE 1 — INGESTÃO (este arquivo):                               ║
 * ║    Load → Split → Embed → Store                                  ║
 * ║                                                                  ║
 * ║  FASE 2 — CONSULTA (RagChatService.java):                        ║
 * ║    Query → Embed → Search → Augment Prompt → LLM → Response     ║
 * ║                                                                  ║
 * ║  Fluxo de ingestão:                                              ║
 * ║  Arquivo → [Tika] → texto → [Splitter] → chunks                 ║
 * ║          → [Embedding Model] → vetores → [pgvector]             ║
 * ║                                                                  ║
 * ║  Por que fatiar em chunks?                                       ║
 * ║  1. Modelos têm limite de tokens no contexto                     ║
 * ║  2. Busca vetorial funciona melhor com trechos específicos       ║
 * ║  3. Chunks menores = respostas mais precisas e focadas           ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final VectorStore vectorStore;

    @Value("${rag.chunk-size:800}")
    private int chunkSize;

    @Value("${rag.chunk-overlap:100}")
    private int chunkOverlap;

    public DocumentIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Ingere um arquivo enviado via upload HTTP (PDF, TXT, DOCX, HTML...).
     *
     * @param file   arquivo do multipart form
     * @param source nome descritivo da fonte (ex: "manual-spring-ai")
     * @return número de chunks indexados
     */
    public int ingest(MultipartFile file, String source) throws IOException {
        log.info("Iniciando ingestão: {} ({} bytes)", file.getOriginalFilename(), file.getSize());

        // ── Etapa 1: LOAD ─────────────────────────────────────────────
        // Apache Tika extrai texto puro de qualquer formato de arquivo.
        // Suporta PDF, DOCX, HTML, Markdown, ODT, EPUB, etc.
        var reader  = new TikaDocumentReader(file.getResource());
        List<Document> rawDocs = reader.get();
        log.info("Tika extraiu {} bloco(s) do arquivo", rawDocs.size());

        // Metadados: ficam salvos junto com cada chunk no pgvector
        // Aparecem no resultado da busca — útil para citar a fonte
        rawDocs.forEach(doc -> doc.getMetadata().putAll(Map.of(
                "source",    source,
                "filename",  file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown",
                "mediaType", file.getContentType()      != null ? file.getContentType()      : "unknown"
        )));

        return splitAndStore(rawDocs, source);
    }

    /**
     * Ingere um arquivo que já está no classpath (src/main/resources/documents/).
     * Permite pré-carregar documentos junto com o .jar sem upload manual.
     *
     * @param resource recurso do classpath (ex: new ClassPathResource("documents/spring-ai.txt"))
     * @param sourceName nome descritivo da fonte
     */
    public int ingestResource(Resource resource, String sourceName) throws IOException {
        log.info("Ingerindo recurso do classpath: {}", sourceName);

        var reader = new TikaDocumentReader(resource);
        List<Document> rawDocs = reader.get();
        rawDocs.forEach(doc -> doc.getMetadata().put("source", sourceName));

        return splitAndStore(rawDocs, sourceName);
    }

    /**
     * Etapas 2 e 3 compartilhadas: Split → Embed → Store
     */
    private int splitAndStore(List<Document> rawDocs, String source) {

        // ── Etapa 2: SPLIT ────────────────────────────────────────────
        // TokenTextSplitter divide por tokens (não caracteres).
        // - chunkSize:    tamanho máximo de cada pedaço em tokens
        // - chunkOverlap: tokens repetidos entre chunks consecutivos
        //                 evita perder contexto na "costura"
        //
        // Exemplo com chunkSize=800, overlap=100:
        //   chunk1: tokens  0..800
        //   chunk2: tokens  700..1500   ← 100 tokens sobrepostos
        //   chunk3: tokens  1400..2200
        var splitter = new TokenTextSplitter(chunkSize, chunkOverlap, 5, 10000, true);
        List<Document> chunks = splitter.apply(rawDocs);
        log.info("'{}' → {} chunks (size={}, overlap={})", source, chunks.size(), chunkSize, chunkOverlap);

        // ── Etapa 3: EMBED + STORE ────────────────────────────────────
        // vectorStore.add() orquestra:
        //   1. Chama nomic-embed-text via Ollama para cada chunk
        //   2. Recebe vetor float[768] para cada chunk
        //   3. INSERT na tabela vector_store do PostgreSQL
        //
        // Este passo pode demorar alguns segundos dependendo do
        // número de chunks e velocidade do Ollama.
        vectorStore.add(chunks);

        log.info("✅ '{}' indexado: {} chunks no pgvector", source, chunks.size());
        return chunks.size();
    }
}
