# feat/03 — Streaming de Respostas com SSE

> Branch anterior: `feat/02-chat-memory` → **este branch**: `feat/03-streaming`

---

## ⚡ O que foi adicionado

### Problema que resolve
O endpoint `/api/chat` bloqueia até o modelo terminar de gerar a resposta inteira (pode levar 5-30 segundos). O usuário fica olhando para um loading sem feedback.

### Solução
`ChatClient.stream()` + `MediaType.TEXT_EVENT_STREAM_VALUE` (SSE) + `fetch()` com `ReadableStream` no frontend.

### Arquivos modificados

| Arquivo | O que mudou |
|---|---|
| `ChatService.java` | Adicionado método `chatStream()` que retorna `Flux<String>` |
| `ChatController.java` | Novo endpoint `POST /api/chat/stream` que produz `text/event-stream` |
| `app.js` | Usa `fetch()` + `ReadableStream` para consumir SSE |
| `index.html` | Badge atualizado para `feat/03 · streaming` |
| `pom.xml` | Adicionado `spring-boot-starter-webflux` |

---

## 🧪 Testando o streaming

```bash
# Veja os tokens chegando em tempo real no terminal!
curl -N -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "explique o que é streaming em 3 parágrafos", "conversationId": "s1"}'

# A flag -N desativa o buffer do curl — você vê cada token conforme chega
```

---

## 💡 Conceitos deste branch

### Server-Sent Events (SSE)
Protocolo HTTP unidirecional onde o servidor envia eventos para o cliente continuamente.
Formato de cada evento: `data: conteúdo\n\n`

### Project Reactor / Flux
`Flux<T>` é um stream assíncrono reativo de N elementos.
O Spring WebFlux sabe serializar um `Flux<String>` como SSE automaticamente.

### ReadableStream (browser)
`response.body` de um `fetch()` é um `ReadableStream`.
O `reader.read()` retorna `{ done, value }` a cada chunk disponível.

---

## ➡️ Próximo passo

`feat/04-rag-documentos` — conecte o assistente a documentos PDF/TXT via busca vetorial
