# 🤖 Spring AI Intro — Portfólio de IA com Java

Projeto introdutório de integração com LLMs usando **Spring Boot + Spring AI + Ollama**.

O objetivo deste projeto é explorar os conceitos fundamentais de aplicações de IA Generativa em Java, utilizando modelos locais executados através do Ollama e integrados ao Spring AI.

---

## 📚 Conceitos abordados neste projeto

| Conceito                 | Onde ver no código                         |
| ------------------------ | ------------------------------------------ |
| **LLM Local**            | Ollama + modelo Qwen/Gemma                 |
| **Prompt Engineering**   | `ChatService.java`                         |
| **System Prompt**        | `ChatService.java` — `SystemMessage`       |
| **Tokens**               | `ChatController.java` — `getTotalTokens()` |
| **Temperatura**          | `application.properties`                   |
| **ChatModel / LLM Call** | `ChatService.java`                         |
| **Prompt Structure**     | `Prompt(List.of(...))`                     |
| **Spring AI**            | Integração entre aplicação e modelo        |

---

## 🚀 Como rodar

### 1. Pré-requisitos

* Java 21+
* Maven 3.9+
* Docker Desktop
* 8 GB de RAM (mínimo recomendado)

---

### 2. Subir o Ollama via Docker

**Windows (PowerShell):**

```powershell
docker run -d --name ollama -p 11434:11434 -v ollama:/root/.ollama ollama/ollama
```

Verifique se o container está ativo:

```powershell
docker ps
```

---

### 3. Baixar um modelo

Modelo recomendado para máquinas mais simples:

```powershell
docker exec -it ollama ollama pull qwen2.5:1.5b
```

Alternativa:

```powershell
docker exec -it ollama ollama pull gemma2:2b
```

Listar modelos instalados:

```powershell
docker exec -it ollama ollama list
```

---

### 4. Rodar a aplicação

```bash
mvn spring-boot:run
```

---

## 🧪 Testando os endpoints

### Teste rápido

```bash
curl http://localhost:8080/api/chat/hello
```

---

### Pergunta via GET

```bash
curl "http://localhost:8080/api/chat?message=o que é Spring AI?"
```

Ou diretamente no navegador:

```text
http://localhost:8080/api/chat?message=o%20que%20%C3%A9%20Spring%20AI
```

---

### Pergunta via POST

```bash
curl -X POST http://localhost:8080/api/chat \
-H "Content-Type: application/json" \
-d '{"message":"Explique records em Java 21"}'
```

---

### Exemplo de resposta

```json
{
  "response": "Records são classes imutáveis introduzidas no Java...",
  "model": "qwen2.5:1.5b",
  "tokensUsed": 145
}
```

---

## 📁 Estrutura do projeto

```text
src/main/java/com/portfolio/aiintro/
├── AiIntroApplication.java
├── controller/
│   ├── ChatController.java
│   ├── ChatRequest.java
│   └── ChatResponse.java
└── service/
    └── ChatService.java
```

---

## ⚙️ Configuração do Ollama

### application.properties

```properties
spring.application.name=ai-intro

server.port=8080

spring.ai.ollama.base-url=http://localhost:11434

spring.ai.ollama.chat.model=qwen2.5:1.5b

spring.ai.ollama.chat.options.temperature=0.7

logging.level.org.springframework.ai=DEBUG
```

---

## 🏗️ Arquitetura

```text
Cliente (Browser/Postman)
            │
            ▼
      Spring Boot
            │
            ▼
       Spring AI
            │
            ▼
  Ollama (localhost:11434)
            │
            ▼
      Modelo Local
    (Qwen / Gemma)
```

---

## 🎯 Objetivos de aprendizado

Este projeto foi criado para praticar:

* Integração com LLMs usando Spring AI
* Engenharia de Prompts
* Consumo de modelos locais
* Estruturação de APIs REST para IA
* Conceitos de IA Generativa
* Preparação para projetos de RAG e Agentes

---

## 🗺️ Próximos passos

* [ ] Memória de conversa com ChatMemory
* [ ] Streaming de respostas
* [ ] RAG com documentos PDF/TXT
* [ ] Tool Calling
* [ ] Function Calling
* [ ] Agente para consulta de APIs externas
* [ ] Vetorização e banco vetorial
* [ ] Comparação entre Ollama, OpenAI e Gemini
* [ ] Testes unitários com mock do ChatModel

---

## 💡 Por que Ollama?

* Gratuito
* Sem API Key
* Sem cobrança por uso
* Funciona offline
* Ideal para estudos e portfólio
* Fácil integração com Spring AI

---

## 🔗 Referências

* Spring AI Documentation
* Ollama Documentation
* Spring AI GitHub
* Ollama GitHub

```
```
