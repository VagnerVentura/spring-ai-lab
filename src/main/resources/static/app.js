document.addEventListener('DOMContentLoaded', () => {
    const chatForm        = document.getElementById('chat-form');
    const userInput       = document.getElementById('user-input');
    const chatContainer   = document.getElementById('chat-container');
    const loadingEl       = document.getElementById('loading-indicator');
    const modelNameEl     = document.getElementById('model-name');
    const tokenUsageEl    = document.getElementById('token-usage');
    const systemStatusEl  = document.getElementById('system-status');
    const systemTimeEl    = document.getElementById('system-time');
    const sessionIdEl     = document.getElementById('session-id-display');
    const clearBtn        = document.getElementById('clear-btn');

    // conversationId é mantido no frontend entre mensagens
    // null = deixa o backend gerar um UUID na primeira mensagem
    let conversationId = null;

    setInterval(() => {
        systemTimeEl.textContent = new Date().toLocaleTimeString('pt-BR');
    }, 1000);

    chatForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const message = userInput.value.trim();
        if (!message) return;

        userInput.value = '';
        addMessage(message, 'user');
        setLoading(true);

        try {
            const res = await fetch('/api/chat', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                // Envia o conversationId atual (null na primeira mensagem)
                body: JSON.stringify({ message, conversationId })
            });

            if (!res.ok) throw new Error('Falha na comunicação.');
            const data = await res.json();

            // Persiste o conversationId para as próximas mensagens
            conversationId = data.conversationId;
            sessionIdEl.textContent = conversationId;

            addMessage(data.response, 'ai');
            modelNameEl.textContent  = data.model;
            tokenUsageEl.textContent = data.tokensUsed;
            setStatus('ONLINE', '#238636');

        } catch (err) {
            addMessage('ERRO: ' + err.message, 'ai');
            setStatus('ERROR', '#f85149');
        } finally {
            setLoading(false);
        }
    });

    // Limpa a memória no backend e reinicia a sessão no frontend
    clearBtn.addEventListener('click', async () => {
        if (!conversationId) return;
        await fetch(`/api/chat/${conversationId}`, { method: 'DELETE' });
        conversationId = null;
        sessionIdEl.textContent = '—';
        chatContainer.innerHTML = '';
        addMessage('Memória limpa. Nova conversa iniciada!', 'ai');
    });

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
        if (on) setStatus('PROCESSING', '#e3b341');
        chatContainer.scrollTop = chatContainer.scrollHeight;
    }

    function setStatus(text, color) {
        systemStatusEl.textContent = text;
        systemStatusEl.style.color = color;
    }
});
