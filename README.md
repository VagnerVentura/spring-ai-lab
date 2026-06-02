# feat/04 — RAG com Documentos (pgvector + Ollama Embeddings)

> Branch anterior: `feat/03-streaming` → **este branch**: `feat/04-rag-documentos`

---

## 🧠 O que é RAG?

**RAG (Retrieval-Augmented Generation)** conecta um LLM a uma base de documentos externa.
Em vez de depender apenas do treinamento do modelo, o RAG:

1. Indexa seus documentos como vetores num banco vetorial (pgvector)
2. Na consulta, busca os trechos mais relevantes para a pergunta
3. Injeta esses trechos no prompt como contexto
4. O modelo responde baseado *apenas* nesse contexto → sem alucinações

```
Pergunta → [Embedding] → vetor → [pgvector busca] → chunks relevantes
        → [Prompt com contexto] → [LLM] → Resposta fundamentada
```

---

## 🏗️ Arquitetura

```
┌──────────────┐     upload      ┌──────────────────────┐
│   Frontend   │ ─────────────▶  │ DocumentIngestionSvc  │
│  (browser)   │                 │  Tika → Split → Embed │
└──────┬───────┘                 └──────────┬────────────┘
       │ pergunta SSE                        │ vetores
       ▼                                     ▼
┌──────────────┐   busca vetorial  ┌────────────────────┐
│ RagChatSvc   │ ◀──────────────── │   pgvector         │
│ QA Advisor   │                   │   (PostgreSQL)     │
│ Memory Adv.  │                   └────────────────────┘
└──────┬───────┘
       │ prompt enriquecido
       ▼
┌──────────────┐
│    Ollama    │  ←  qwen2.5:1.5b (chat)
│              │  ←  nomic-embed-text (embeddings)
└──────────────┘
```

---

## 🚀 Como rodar

### 1. Subir a infraestrutura

```bash
# Na pasta src/main/resources/
docker compose up -d

# Verifique os containers
docker ps
```

### 2. Baixar os modelos no Ollama

```bash
# Modelo de chat
docker exec -it rag-ollama ollama pull qwen2.5:1.5b

# Modelo de embeddings (OBRIGATÓRIO — diferente do chat!)
docker exec -it rag-ollama ollama pull nomic-embed-text
```

### 3. Rodar a aplicação

```bash
mvn spring-boot:run
```

O `StartupDocumentLoader` indexará automaticamente o arquivo
`documents/spring-ai-overview.txt` ao iniciar.

---

## 🧪 Testando o RAG

### Ingestão de documento
```bash
curl -X POST http://localhost:8080/api/rag/ingest \
  -F "file=@meu-documento.pdf" \
  -F "source=meu-documento"
```

### Pergunta com RAG
```bash
curl -X POST http://localhost:8080/api/rag/ask \
  -H "Content-Type: application/json" \
  -d '{"message": "O que é o QuestionAnswerAdvisor?", "conversationId": "s1"}'
```

### Pergunta com RAG + streaming
```bash
curl -N -X POST http://localhost:8080/api/rag/ask/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "explique o pipeline de ingestão RAG", "conversationId": "s1"}'
```

### Busca vetorial pura (debug)
```bash
# Veja exatamente quais chunks o RAG está encontrando
curl "http://localhost:8080/api/rag/search?q=embeddings+e+vetores"
```

---

## 📁 Arquivos novos neste branch

| Arquivo | Função |
|---|---|
| `rag/DocumentIngestionService.java` | Pipeline Load → Split → Embed → Store |
| `rag/RagChatService.java` | Consulta RAG com QuestionAnswerAdvisor |
| `rag/StartupDocumentLoader.java` | Carrega documento de exemplo ao iniciar |
| `controller/RagController.java` | Endpoints /ingest, /ask, /ask/stream, /search |
| `resources/docker-compose.yml` | pgvector + Ollama via Docker |
| `resources/init.sql` | Habilita extensão vector no PostgreSQL |
| `resources/documents/spring-ai-overview.txt` | Documento de exemplo pré-indexado |

---

## 💡 Conceitos deste branch

| Conceito | Onde ver |
|---|---|
| **Embeddings** | `DocumentIngestionService` — `vectorStore.add()` |
| **Chunking** | `DocumentIngestionService` — `TokenTextSplitter` |
| **VectorStore** | `application.properties` — pgvector config |
| **Similaridade cosseno** | `RagChatService` — `SearchRequest` |
| **QuestionAnswerAdvisor** | `RagChatService.ask()` |
| **Threshold de similaridade** | `RagChatService` — `similarityThreshold(0.5)` |
| **Metadados de chunks** | `DocumentIngestionService` — `doc.getMetadata()` |

---

## ➡️ Próximo passo

`feat/05-tool-calling` — o modelo decide autonomamente quais funções Java chamar
