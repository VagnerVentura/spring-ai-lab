package com.portfolio.aiintro.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  StartupDocumentLoader                                       ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  Roda uma vez ao subir a aplicação (ApplicationRunner).      ║
 * ║                                                              ║
 * ║  Carrega automaticamente o arquivo                           ║
 * ║  src/main/resources/documents/spring-ai-overview.txt        ║
 * ║  no pgvector para que o RAG já tenha conteúdo               ║
 * ║  sem precisar fazer upload manual.                           ║
 * ║                                                              ║
 * ║  Em produção, você pode:                                     ║
 * ║  - Remover este componente e usar só o endpoint /ingest      ║
 * ║  - Adicionar verificação se o documento já foi indexado      ║
 * ║    (ex: buscar por metadado source == "spring-ai-overview")  ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
@Component
public class StartupDocumentLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupDocumentLoader.class);

    private final DocumentIngestionService ingestionService;

    public StartupDocumentLoader(DocumentIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            var resource = new ClassPathResource("documents/spring-ai-overview.txt");

            if (!resource.exists()) {
                log.warn("Arquivo de exemplo não encontrado: documents/spring-ai-overview.txt");
                log.warn("Crie o arquivo ou faça upload via POST /api/rag/ingest");
                return;
            }

            log.info("📄 Carregando documento de exemplo ao iniciar...");
            int chunks = ingestionService.ingestResource(resource, "spring-ai-overview");
            log.info("✅ Documento de exemplo indexado em {} chunks. RAG pronto!", chunks);

        } catch (Exception e) {
            // Não deixa a aplicação cair se o documento não puder ser indexado
            log.error("Falha ao carregar documento de exemplo: {}", e.getMessage());
            log.info("A aplicação continuará — faça upload via POST /api/rag/ingest");
        }
    }
}
