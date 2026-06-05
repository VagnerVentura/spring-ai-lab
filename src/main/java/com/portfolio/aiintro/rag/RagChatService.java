package com.portfolio.aiintro.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║  CONCEITO: Fase de Consulta do RAG (Retrieval + Generation)     ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║                                                                  ║
 * ║  Quando o usuário faz uma pergunta, o RAG:                       ║
 * ║                                                                  ║
 * ║  1. EMBED da pergunta                                            ║
 * ║     A pergunta vira um vetor usando o mesmo modelo de embedding  ║
 * ║                                                                  ║
 * ║  2. RETRIEVAL (Busca Vetorial)                                   ║
 * ║     Compara o vetor da pergunta com todos os vetores no pgvector ║
 * ║     Retorna os top-K chunks mais similares (similaridade coseno) ║
 * ║                                                                  ║
 * ║  3. AUGMENTATION (Enriquecimento do Prompt)                      ║
 * ║     Os chunks encontrados são injetados no prompt como contexto: ║
 * ║     "Use o contexto abaixo para responder: [chunks] Pergunta: X" ║
 * ║                                                                  ║
 * ║  4. GENERATION                                                   ║
 * ║     O LLM gera a resposta baseada APENAS no contexto fornecido   ║
 * ║     → evita alucinações sobre o conteúdo do documento           ║
 * ║                                                                  ║
 * ║  O QuestionAnswerAdvisor do Spring AI faz as etapas 1-3          ║
 * ║  automaticamente a cada chamada.                                 ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
@Service
public class RagChatService {

    private static final Logger log = LoggerFactory.getLogger(RagChatService.class);

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ChatMemory  chatMemory;

    @Value("${rag.topk:5}")
    private int topK;

    public RagChatService(ChatClient.Builder builder,
                          VectorStore vectorStore,
                          ChatMemory chatMemory) {
        this.vectorStore = vectorStore;
        this.chatMemory  = chatMemory;

        this.chatClient = builder
                .defaultSystem("""
                        Você é um assistente especializado nos documentos que foram indexados.
                        
                        REGRAS IMPORTANTES:
                        - Responda APENAS com base no contexto fornecido abaixo.
                        - Se a informação não estiver no contexto, diga claramente:
                          "Não encontrei essa informação nos documentos indexados."
                        - Não invente informações. Não use conhecimento externo.
                        - Cite qual parte do documento embasou sua resposta.
                        - Responda sempre em português brasileiro.
                        - Seja objetivo e didático.
                        """)
                .build();
    }

    /**
     * Pergunta com RAG — resposta completa (bloqueante).
     *
     * Os dois advisors trabalham em sequência:
     * 1. QuestionAnswerAdvisor: busca os chunks relevantes no pgvector
     *    e injeta no prompt como contexto
     * 2. MessageChatMemoryAdvisor: injeta o histórico da conversa
     *
     * @param question       pergunta do usuário
     * @param conversationId ID da sessão para memória
     */
    public ChatResponse ask(String question, String conversationId) {
        log.debug("RAG query – topK={}, conversationId={}", topK, conversationId);

        return chatClient.prompt()
                .user(question)
                .advisors(injectAdvisors(conversationId))
                .call()
                .chatResponse();
    }

    private Advisor[] injectAdvisors(String conversationId) {
        // Advisor 1: RAG clássico de documentos (Aceita o 'new' pois tem construtor público customizado no Builder)
       var questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
               .searchRequest(SearchRequest.builder()
                        .topK(topK)
                        .similarityThreshold(0.5)
                        .build())
               .build();

       // Advisor 2: Memória curta/normal da conversa atual
       var messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                        .conversationId(conversationId)
                        .order(10)
                        .build();

//TODO Se você quer que o Histórico de Conversa seja buscado no VectorStore
        var vectorStoreChatMemoryAdvisor = VectorStoreChatMemoryAdvisor.builder(vectorStore)
                                .defaultTopK(topK)
                                .conversationId(conversationId)
                                .build();

        return new Advisor[]{questionAnswerAdvisor,messageChatMemoryAdvisor, vectorStoreChatMemoryAdvisor};//
    }

    /**
     * Pergunta com RAG — resposta em streaming (token a token).
     * Combina RAG + memória + streaming simultaneamente.
     */
    public Flux<String> askStream(String question, String conversationId) {
        return chatClient.prompt()
                .user(question)
                .advisors(injectAdvisors(conversationId))
                .stream()
                .content();
    }

    /**
     * Busca vetorial pura — sem LLM, só retorna os chunks mais similares.
     * Útil para depurar: ver exatamente o que o RAG está encontrando.
     *
     * Acesse: GET /api/rag/search?q=sua+pergunta
     */
    public java.util.List<org.springframework.ai.document.Document> search(String query) {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(0.4)
                        .build()
        );
    }
}
