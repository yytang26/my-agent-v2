const API_BASE = '';

let currentSessionId = null;
let isStreaming = false;
let currentAssistantBubble = null;
let currentAssistantText = '';

// ========== Init ==========
document.addEventListener('DOMContentLoaded', () => {
    loadSessions();
    loadCost();

    document.getElementById('new-session-btn').addEventListener('click', createSession);
    document.getElementById('send-btn').addEventListener('click', sendMessage);
    document.getElementById('message-input').addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && e.ctrlKey) {
            sendMessage();
        }
    });
});

// ========== Sessions ==========
async function loadSessions() {
    try {
        const res = await fetch(`${API_BASE}/api/sessions`);
        const sessions = await res.json();
        renderSessionList(sessions);
    } catch (e) {
        console.error('加载会话失败', e);
    }
}

function renderSessionList(sessions) {
    const list = document.getElementById('session-list');
    list.innerHTML = '';
    sessions.forEach(s => {
        const item = document.createElement('div');
        item.className = 'session-item' + (s.sessionId === currentSessionId ? ' active' : '');
        item.innerHTML = `
            <span class="session-title">${escapeHtml(s.title || '新会话')}</span>
            <button class="session-delete" title="删除">&times;</button>
        `;
        item.querySelector('.session-title').addEventListener('click', () => switchSession(s.sessionId));
        item.querySelector('.session-delete').addEventListener('click', (e) => {
            e.stopPropagation();
            deleteSession(s.sessionId);
        });
        list.appendChild(item);
    });
}

async function createSession() {
    try {
        const res = await fetch(`${API_BASE}/api/sessions`, { method: 'POST' });
        const data = await res.json();
        currentSessionId = data.sessionId;
        document.getElementById('messages').innerHTML = '';
        renderEmptyState();
        loadSessions();
    } catch (e) {
        console.error('创建会话失败', e);
    }
}

async function switchSession(sessionId) {
    currentSessionId = sessionId;
    loadSessions();
    try {
        const res = await fetch(`${API_BASE}/api/sessions/${sessionId}/messages`);
        const messages = await res.json();
        renderMessages(messages);
    } catch (e) {
        console.error('加载消息失败', e);
    }
}

async function deleteSession(sessionId) {
    try {
        await fetch(`${API_BASE}/api/sessions/${sessionId}`, { method: 'DELETE' });
        if (currentSessionId === sessionId) {
            currentSessionId = null;
            document.getElementById('messages').innerHTML = '';
            renderEmptyState();
        }
        loadSessions();
    } catch (e) {
        console.error('删除会话失败', e);
    }
}

// ========== Messages ==========
function renderMessages(messages) {
    const container = document.getElementById('messages');
    container.innerHTML = '';
    messages.forEach(m => appendMessage(m.role, m.content));
    scrollToBottom();
}

function renderEmptyState() {
    const container = document.getElementById('messages');
    if (container.children.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <h2>欢迎使用 My Agent</h2>
                <p>在下方输入框中发送消息开始对话</p>
            </div>
        `;
    }
}

function appendMessage(role, content, html = null) {
    const container = document.getElementById('messages');
    const empty = container.querySelector('.empty-state');
    if (empty) empty.remove();

    const msgDiv = document.createElement('div');
    msgDiv.className = `message ${role}`;
    const bubble = document.createElement('div');
    bubble.className = 'message-bubble';
    if (html) {
        bubble.innerHTML = html;
    } else if (role === 'assistant') {
        bubble.innerHTML = renderMarkdown(content);
    } else {
        bubble.textContent = content;
    }
    msgDiv.appendChild(bubble);
    container.appendChild(msgDiv);
    scrollToBottom();
    return bubble;
}

function scrollToBottom() {
    const container = document.getElementById('messages');
    container.scrollTop = container.scrollHeight;
}

// ========== Send & SSE ==========
async function sendMessage() {
    const input = document.getElementById('message-input');
    const text = input.value.trim();
    if (!text || isStreaming) return;

    if (!currentSessionId) {
        await createSession();
    }

    input.value = '';
    appendMessage('user', text);
    isStreaming = true;
    setInputEnabled(false);

    currentAssistantText = '';
    currentAssistantBubble = appendMessage('assistant', '', '');

    try {
        const response = await fetch(`${API_BASE}/api/chat`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessionId: currentSessionId, message: text })
        });

        if (!response.ok) {
            throw new Error('HTTP ' + response.status);
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            buffer += decoder.decode(value, { stream: true });

            const lines = buffer.split('\n');
            buffer = lines.pop(); // keep incomplete line in buffer

            for (let i = 0; i < lines.length; i++) {
                const line = lines[i].trim();
                if (!line) continue;

                if (line.startsWith('event:')) {
                    const eventName = line.substring(6).trim();
                    // next line should be data:
                    const dataLine = lines[++i];
                    if (dataLine && dataLine.startsWith('data:')) {
                        const data = dataLine.substring(5).trim();
                        handleSseEvent(eventName, data);
                    }
                }
            }
        }

        // process remaining buffer
        if (buffer.trim()) {
            const lines = buffer.split('\n');
            for (let i = 0; i < lines.length; i++) {
                const line = lines[i].trim();
                if (line.startsWith('event:')) {
                    const eventName = line.substring(6).trim();
                    const dataLine = lines[++i];
                    if (dataLine && dataLine.startsWith('data:')) {
                        const data = dataLine.substring(5).trim();
                        handleSseEvent(eventName, data);
                    }
                }
            }
        }
    } catch (e) {
        console.error('发送消息失败', e);
        currentAssistantBubble.innerHTML = `<p style="color:#ef4444">错误: ${escapeHtml(e.message)}</p>`;
    } finally {
        isStreaming = false;
        setInputEnabled(true);
        loadCost();
    }
}

function handleSseEvent(eventName, data) {
    try {
        const payload = JSON.parse(data);
        switch (eventName) {
            case 'text': {
                const delta = payload.delta || '';
                currentAssistantText += delta;
                currentAssistantBubble.innerHTML = renderMarkdown(currentAssistantText);
                scrollToBottom();
                break;
            }
            case 'tool_start': {
                const toolId = payload.id || payload.name;
                const toolDiv = document.createElement('div');
                toolDiv.className = 'tool-call status-running';
                toolDiv.id = `tool-${toolId}`;
                toolDiv.innerHTML = `
                    <div class="tool-call-header">
                        <span class="tool-icon">&#128295;</span>
                        <span class="tool-name">${escapeHtml(payload.name)}</span>
                        <span class="tool-toggle">&#9660;</span>
                    </div>
                    <div class="tool-call-details">
                        <pre><code>${escapeHtml(JSON.stringify(payload.arguments || {}, null, 2))}</code></pre>
                    </div>
                `;
                toolDiv.querySelector('.tool-call-header').addEventListener('click', () => {
                    toolDiv.querySelector('.tool-call-details').classList.toggle('open');
                });
                currentAssistantBubble.appendChild(toolDiv);
                scrollToBottom();
                break;
            }
            case 'tool_result': {
                const toolId = payload.id || payload.name;
                const toolDiv = document.getElementById(`tool-${toolId}`);
                if (toolDiv) {
                    toolDiv.classList.remove('status-running');
                    toolDiv.classList.add(payload.error ? 'status-error' : 'status-done');
                    const details = toolDiv.querySelector('.tool-call-details');
                    details.innerHTML += `
                        <div style="margin-top:6px;color:${payload.error ? '#ef4444' : '#22c55e'}">
                            <strong>结果:</strong>
                        </div>
                        <pre><code>${escapeHtml(payload.result || '')}</code></pre>
                    `;
                }
                scrollToBottom();
                break;
            }
            case 'done':
                break;
            case 'error':
                currentAssistantBubble.innerHTML += `<p style="color:#ef4444">错误: ${escapeHtml(payload.message || '')}</p>`;
                scrollToBottom();
                break;
        }
    } catch (e) {
        console.warn('解析 SSE 数据失败', e, data);
    }
}

function setInputEnabled(enabled) {
    document.getElementById('message-input').disabled = !enabled;
    document.getElementById('send-btn').disabled = !enabled;
}

// ========== Markdown ==========
function renderMarkdown(text) {
    if (!text) return '';
    const raw = marked.parse(text);
    // wrap in a temp div to apply highlighting
    const div = document.createElement('div');
    div.innerHTML = raw;
    div.querySelectorAll('pre code').forEach((block) => {
        hljs.highlightElement(block);
    });
    return div.innerHTML;
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ========== Cost ==========
async function loadCost() {
    try {
        const res = await fetch(`${API_BASE}/api/cost`);
        const data = await res.json();
        const el = document.getElementById('cost-info');
        el.textContent = `Tokens: ${data.totalTokens || 0} | $${(data.totalCostUsd || 0).toFixed(4)}`;
    } catch (e) {
        console.error('加载费用失败', e);
    }
}
