# feat/02 — Memória de Conversa com ChatMemory

> Branch anterior: `feat/01-chat-basico` → **este branch**: `feat/02-chat-memory`

---

## 🧠 O que foi adicionado

### Problema que resolve
LLMs são **stateless** — cada chamada à API é independente. Sem memória, o modelo esquece tudo entre mensagens.

### Solução
`InMemoryChatMemory` + `MessageChatMemoryAdvisor` do Spring AI.
O advisor intercepta cada chamada, injeta o histórico no prompt e salva a nova troca automaticamente.

### Arquivos modificados

| Arquivo | O que mudou |
|---|---|
| `ChatMemoryConfig.java` | **NOVO** — registra `InMemoryChatMemory` como bean |
| `ChatService.java` | Migrou de `OllamaChatModel` para `ChatClient` + advisor |
| `ChatRequest.java` | Adicionado campo `conversationId` |
| `ChatResponse.java` | Adicionado campo `conversationId` na resposta |
| `ChatController.java` | Novo endpoint `DELETE /{conversationId}` para limpar memória |
| `app.js` | Frontend mantém o `conversationId` entre mensagens |
| `pom.xml` | Adicionado `spring-ai-advisors-vector-store` |

---

## 🧪 Testando a memória

```bash
# Mensagem 1 - apresentação
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "meu nome é Vagner", "conversationId": "sessao-teste"}'

# Mensagem 2 - teste de memória (mesmo conversationId)
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "qual é o meu nome?", "conversationId": "sessao-teste"}'

# Mensagem 3 - outra sessão (não deve saber o nome)
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "qual é o meu nome?", "conversationId": "outra-sessao"}'

# Limpar memória de uma sessão
curl -X DELETE http://localhost:8080/api/chat/sessao-teste
```

---

## 💡 Conceito: por que o modelo "lembra"?

O modelo não guarda nada. O `MessageChatMemoryAdvisor` **reenvia o histórico completo** a cada nova mensagem:

```
[system]: Você é um assistente Java...
[user]:   meu nome é Vagner          ← histórico da msg anterior
[assistant]: Olá Vagner! Como posso... ← resposta anterior
[user]:   qual é o meu nome?         ← nova mensagem
```

É por isso que o **context window** importa — quanto mais longo o histórico, mais tokens são consumidos.

---

## ➡️ Próximo passo

`feat/03-streaming` — respostas aparecem token a token (efeito "digitando")
