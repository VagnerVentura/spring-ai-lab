document.addEventListener('DOMContentLoaded', () => {
    const chatForm = document.getElementById('chat-form');
    const userInput = document.getElementById('user-input');
    const chatContainer = document.getElementById('chat-container');
    const loadingIndicator = document.getElementById('loading-indicator');
    const modelNameEl = document.getElementById('model-name');
    const tokenUsageEl = document.getElementById('token-usage');
    const systemStatusEl = document.getElementById('system-status');
    const systemTimeEl = document.getElementById('system-time');

    // Atualiza relógio do sistema
    setInterval(() => {
        const now = new Date();
        systemTimeEl.textContent = now.toLocaleTimeString('pt-BR');
    }, 1000);

    chatForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const message = userInput.value.trim();
        if (!message) return;

        // Limpa input e adiciona mensagem do usuário
        userInput.value = '';
        addMessage(message, 'user');
        
        // Inicia carregamento
        setLoading(true);

        try {
            const response = await fetch('/api/chat', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ message })
            });

            if (!response.ok) throw new Error('Falha na comunicação com o laboratório.');

            const data = await response.json();
            
            // Adiciona resposta da IA
            addMessage(data.response, 'ai');
            
            // Atualiza metadados
            modelNameEl.textContent = data.model;
            tokenUsageEl.textContent = data.tokensUsed;
            systemStatusEl.textContent = 'ONLINE';
            systemStatusEl.style.color = '#238636';

        } catch (error) {
            console.error('Error:', error);
            addMessage('ERRO: ' + error.message, 'ai');
            systemStatusEl.textContent = 'ERROR';
            systemStatusEl.style.color = '#f85149';
        } finally {
            setLoading(false);
        }
    });

    function addMessage(text, sender) {
        const messageDiv = document.createElement('div');
        messageDiv.classList.add('message', `${sender}-message`);
        
        const bubbleDiv = document.createElement('div');
        bubbleDiv.classList.add('bubble');
        
        const p = document.createElement('p');
        p.textContent = text;
        
        bubbleDiv.appendChild(p);
        messageDiv.appendChild(bubbleDiv);
        chatContainer.appendChild(messageDiv);
        
        // Auto-scroll para o final
        chatContainer.scrollTop = chatContainer.scrollHeight;
    }

    function setLoading(isLoading) {
        loadingIndicator.style.display = isLoading ? 'block' : 'none';
        userInput.disabled = isLoading;
        document.getElementById('send-btn').disabled = isLoading;
        
        if (isLoading) {
            systemStatusEl.textContent = 'PROCESSING';
            systemStatusEl.style.color = '#e3b341';
        }
        
        chatContainer.scrollTop = chatContainer.scrollHeight;
    }
});
