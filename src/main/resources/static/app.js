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
    const fileInput      = document.getElementById('file-input');
    const uploadArea     = document.getElementById('upload-area');
    const uploadStatus   = document.getElementById('upload-status');
    const docsIndexedEl  = document.getElementById('docs-indexed');

    let conversationId = crypto.randomUUID();
    let docsIndexed    = 0;
    sessionIdEl.textContent = conversationId;

    setInterval(() => {
        systemTimeEl.textContent = new Date().toLocaleTimeString('pt-BR');
    }, 1000);

    // ── Upload: drag-and-drop ────────────────────────────────────
    uploadArea.addEventListener('dragover', e => {
        e.preventDefault();
        uploadArea.classList.add('drag-over');
    });
    uploadArea.addEventListener('dragleave', () => uploadArea.classList.remove('drag-over'));
    uploadArea.addEventListener('drop', e => {
        e.preventDefault();
        uploadArea.classList.remove('drag-over');
        const file = e.dataTransfer.files[0];
        if (file) uploadFile(file);
    });
    fileInput.addEventListener('change', () => {
        if (fileInput.files[0]) uploadFile(fileInput.files[0]);
    });

    async function uploadFile(file) {
        const sourceName = file.name.replace(/\.[^.]+$/, ''); // remove extensão
        setUploadStatus(`Indexando "${file.name}"...`, 'loading');

        const formData = new FormData();
        formData.append('file', file);
        formData.append('source', sourceName);

        try {
            const res = await fetch('/api/rag/ingest', {
                method: 'POST',
                body: formData
            });
            const data = await res.json();

            if (res.ok) {
                docsIndexed += data.chunks;
                docsIndexedEl.textContent = docsIndexed;
                setUploadStatus(
                    `✅ "${data.filename}" indexado — ${data.chunks} chunks criados`,
                    'success'
                );
                addMessage(`Documento "${data.filename}" indexado com sucesso! ${data.chunks} chunks no pgvector. Pode perguntar sobre ele agora.`, 'ai');
            } else {
                setUploadStatus(`❌ Erro: ${data.message}`, 'error');
            }
        } catch (err) {
            setUploadStatus(`❌ Falha no upload: ${err.message}`, 'error');
        }
    }

    function setUploadStatus(msg, type) {
        uploadStatus.textContent = msg;
        uploadStatus.className = `upload-status ${type}`;
    }

    // ── Chat com streaming RAG ───────────────────────────────────
    chatForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const message = userInput.value.trim();
        if (!message) return;

        userInput.value = '';
        addMessage(message, 'user');
        setLoading(true);

        const aiBubble = createStreamingBubble();

        try {
            // Usa o endpoint de streaming RAG
            const response = await fetch('/api/rag/ask/stream', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ message, conversationId })
            });

            if (!response.ok) throw new Error('Falha na consulta RAG.');

            const reader  = response.body.getReader();
            const decoder = new TextDecoder();
            let tokenCount = 0;

            setStatus('STREAMING', '#e3b341');
            setLoading(false);

            while (true) {
                const { done, value } = await reader.read();
                if (done) break;

                const chunk = decoder.decode(value, { stream: true });
                const lines = chunk.split('\n');
                for (const line of lines) {
                    if (line.startsWith('data: ')) {
                        appendToken(aiBubble, line.slice(6));
                        tokenCount++;
                        tokenUsageEl.textContent = `~${tokenCount} chunks`;
                    }
                }
            }

            aiBubble.classList.remove('streaming-cursor');
            modelNameEl.textContent = 'ollama + pgvector';
            setStatus('ONLINE', '#238636');

        } catch (err) {
            aiBubble.textContent = 'ERRO: ' + err.message;
            aiBubble.classList.remove('streaming-cursor');
            setStatus('ERROR', '#f85149');
            setLoading(false);
        }
    });

    clearBtn.addEventListener('click', async () => {
        await fetch(`/api/chat/${conversationId}`, { method: 'DELETE' });
        conversationId = crypto.randomUUID();
        sessionIdEl.textContent = conversationId;
        chatContainer.innerHTML = '';
        addMessage('Memória limpa. Nova sessão iniciada — os documentos indexados permanecem disponíveis.', 'ai');
    });

    function createStreamingBubble() {
        const div    = document.createElement('div');
        div.classList.add('message', 'ai-message');
        const bubble = document.createElement('div');
        bubble.classList.add('bubble', 'streaming-cursor');
        div.appendChild(bubble);
        chatContainer.appendChild(div);
        chatContainer.scrollTop = chatContainer.scrollHeight;
        return bubble;
    }

    function appendToken(bubble, token) {
        bubble.textContent += token;
        chatContainer.scrollTop = chatContainer.scrollHeight;
    }

    function addMessage(text, sender) {
        const div    = document.createElement('div');
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
