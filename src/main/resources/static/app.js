document.addEventListener('DOMContentLoaded', () => {
    const chatForm       = document.getElementById('chat-form');
    const userInput      = document.getElementById('user-input');
    const chatContainer  = document.getElementById('chat-container');
    const loadingEl      = document.getElementById('loading-indicator');
    const modelNameEl    = document.getElementById('model-name');
    const tokenUsageEl   = document.getElementById('token-usage');
    const systemStatusEl = document.getElementById('system-status');
    const systemTimeEl   = document.getElementById('system-time');
    const sessionIdEl    = document.getElementById('session-id-display');
    const clearBtn       = document.getElementById('clear-btn');

    let conversationId = null;

    setInterval(() => {
        systemTimeEl.textContent = new Date().toLocaleTimeString('pt-BR');
    }, 1000);

    chatForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const message = userInput.value.trim();
        if (!message) return;

        // Gera um conversationId no frontend se ainda não existe
        if (!conversationId) {
            conversationId = crypto.randomUUID();
            sessionIdEl.textContent = conversationId;
        }

        userInput.value = '';
        addMessage(message, 'user');
        setLoading(true);

        // Cria a bolha da IA vazia — vamos preenchê-la token a token
        const aiBubble = createStreamingBubble();

        try {
            /**
             * CONCEITO: Fetch com ReadableStream
             *
             * fetch() com streaming não espera a resposta completa.
             * response.body é um ReadableStream que emite chunks conforme chegam.
             *
             * TextDecoder converte os bytes de cada chunk em string.
             * Cada chunk pode conter múltiplos tokens ou um fragmento de token.
             */
            const response = await fetch('/api/chat/stream', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ message, conversationId })
            });

            if (!response.ok) throw new Error('Falha no streaming.');

            const reader  = response.body.getReader();
            const decoder = new TextDecoder();
            let tokenCount = 0;

            setStatus('STREAMING', '#e3b341');
            setLoading(false);

            // Loop que lê o stream chunk a chunk até o servidor fechar a conexão
            while (true) {
                const { done, value } = await reader.read();
                if (done) break;

                // Decodifica o chunk e processa cada linha SSE
                // Formato SSE: "data: texto\n\n"
                const chunk = decoder.decode(value, { stream: true });
                const lines = chunk.split('\n');

                for (const line of lines) {
                    // Remove o prefixo "data: " do protocolo SSE
                    if (line.startsWith('data: ')) {
                        const token = line.slice(6); // remove "data: "
                        appendToken(aiBubble, token);
                        tokenCount++;
                        tokenUsageEl.textContent = `~${tokenCount} chunks`;
                    }
                }
            }

            // Remove cursor piscante ao finalizar
            aiBubble.classList.remove('streaming-cursor');
            modelNameEl.textContent = 'ollama (stream)';
            setStatus('ONLINE', '#238636');

        } catch (err) {
            aiBubble.textContent = 'ERRO: ' + err.message;
            aiBubble.classList.remove('streaming-cursor');
            setStatus('ERROR', '#f85149');
            setLoading(false);
        }
    });

    clearBtn.addEventListener('click', async () => {
        if (!conversationId) return;
        await fetch(`/api/chat/${conversationId}`, { method: 'DELETE' });
        conversationId = null;
        sessionIdEl.textContent = '—';
        chatContainer.innerHTML = '';
        addMessage('Memória limpa. Nova conversa iniciada!', 'ai');
    });

    /** Cria uma bolha vazia com cursor piscante para o streaming */
    function createStreamingBubble() {
        const div = document.createElement('div');
        div.classList.add('message', 'ai-message');
        const bubble = document.createElement('div');
        bubble.classList.add('bubble', 'streaming-cursor');
        div.appendChild(bubble);
        chatContainer.appendChild(div);
        chatContainer.scrollTop = chatContainer.scrollHeight;
        return bubble;
    }

    /** Adiciona um token à bolha em tempo real */
    function appendToken(bubble, token) {
        bubble.textContent += token;
        chatContainer.scrollTop = chatContainer.scrollHeight;
    }

    function addMessage(text, sender) {
        const div = document.createElement('div');
        div.classList.add('message', `${sender}-message`);
        const bubble = document.createElement('div');
        bubble.classList.add('bubble');
        bubble.textContent = text;
        div.appendChild(bubble);
        chatContainer.appendChild(div);
        chatContainer.scrollTop = chatContainer.scrollHeight;
    }

    function setLoading(on) {
        loadingEl.style.display = on ? 'block' : 'none';
        userInput.disabled = on;
        document.getElementById('send-btn').disabled = on;
        chatContainer.scrollTop = chatContainer.scrollHeight;
    }

    function setStatus(text, color) {
        systemStatusEl.textContent = text;
        systemStatusEl.style.color = color;
    }
});
