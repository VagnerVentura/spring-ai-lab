# 🤖 Spring AI Intro — Portfólio de IA com Java

Projeto introdutório de integração com LLMs usando **Spring Boot + Spring AI + Anthropic Claude**.

---

## 📚 Conceitos abordados neste projeto

| Conceito | Onde ver no código |
|---|---|
| **API Key / autenticação** | `application.properties` |
| **System prompt** | `ChatService.java` — `SystemMessage` |
| **Tokens** | `ChatController.java` — `getTotalTokens()` |
| **Temperatura** | `application.properties` — `temperature` |
| **ChatModel / LLM call** | `ChatService.java` — `chatModel.call(prompt)` |
| **Prompt structure** | `ChatService.java` — `Prompt(List.of(...))` |

---

## 🚀 Como rodar

### 1. Pré-requisitos
- Java 21+
- Maven 3.9+
- Conta na [Anthropic Console](https://console.anthropic.com) (plano gratuito disponível)

### 2. Obter a API Key
Acesse https://console.anthropic.com → API Keys → Create Key

### 3. Configurar a chave de ambiente

**Linux / macOS:**
```bash
export ANTHROPIC_API_KEY=sk-ant-sua-chave-aqui
```

**Windows (PowerShell):**
```powershell
$env:ANTHROPIC_API_KEY="sk-ant-sua-chave-aqui"
```

### 4. Rodar o projeto
```bash
mvn spring-boot:run
```

---

## 🧪 Testando os endpoints

### Teste rápido (browser ou curl)
```bash
curl http://localhost:8080/api/chat/hello
```

### Pergunta via GET (fácil de testar no browser)
```bash
curl "http://localhost:8080/api/chat?message=o que é um context window?"
```

### Pergunta completa via POST (retorna metadados)
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Explique records em Java 21 com um exemplo"}'
```

**Resposta esperada:**
```json
{
  "response": "Records em Java 21 são...",
  "model": "claude-3-5-haiku-20241022",
  "tokensUsed": 312
}
```

---

## 📁 Estrutura do projeto

```
src/main/java/com/portfolio/aiintro/
├── AiIntroApplication.java        # Entry point
├── controller/
│   ├── ChatController.java        # Endpoints REST
│   ├── ChatRequest.java           # DTO entrada
│   └── ChatResponse.java          # DTO saída
└── service/
    └── ChatService.java           # Lógica de chamada ao modelo
```

---

## ⚙️ Parâmetros importantes (application.properties)

```properties
# Modelo — opções Anthropic:
# claude-3-5-haiku-20241022   → mais rápido e barato (ideal para testes)
# claude-3-5-sonnet-20241022  → mais capaz
spring.ai.anthropic.chat.model=claude-3-5-haiku-20241022

# Temperatura: 0.0 (determinístico) → 1.0 (mais criativo)
spring.ai.anthropic.chat.options.temperature=0.7

# Tamanho máximo da resposta em tokens (~750 palavras por 1000 tokens)
spring.ai.anthropic.chat.options.max-tokens=1024
```

---

## 🗺️ Próximos passos (evolução do projeto)

- [ ] **Memória de conversa** — adicionar `ChatMemory` para manter histórico
- [ ] **Streaming** — devolver a resposta token a token com `StreamingChatModel`
- [ ] **RAG básico** — carregar um arquivo `.txt` e perguntar sobre ele
- [ ] **System prompt dinâmico** — receber persona via parâmetro da requisição
- [ ] **Múltiplos modelos** — comparar respostas de Claude vs OpenAI
- [ ] **Testes** — unit test do service mockando o `ChatModel`

---

## 🔗 Referências

- [Spring AI Docs](https://docs.spring.io/spring-ai/reference/)
- [Anthropic API Docs](https://docs.anthropic.com)
- [Spring AI GitHub](https://github.com/spring-projects/spring-ai)
