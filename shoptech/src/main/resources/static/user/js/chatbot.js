(() => {
    const launcher = document.querySelector('.shoptech-chat__launcher');
    const panel = document.getElementById('shoptech-chat-panel');
    const closeButton = document.querySelector('.shoptech-chat__close');
    const form = document.getElementById('shoptech-chat-form');
    const input = document.getElementById('shoptech-chat-input');
    const messagesElement = document.getElementById('shoptech-chat-messages');

    if (!launcher || !panel || !closeButton || !form || !input || !messagesElement) {
        return;
    }

    const history = [];
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    function setOpen(isOpen) {
        panel.hidden = !isOpen;
        launcher.setAttribute('aria-expanded', String(isOpen));
        if (isOpen) {
            input.focus();
        }
    }

    function addMessage(text, type, extraClass = '') {
        const message = document.createElement('p');
        message.className = `shoptech-chat__bubble shoptech-chat__bubble--${type} ${extraClass}`.trim();
        message.textContent = text;
        messagesElement.append(message);
        messagesElement.scrollTop = messagesElement.scrollHeight;
        return message;
    }

    launcher.addEventListener('click', () => setOpen(panel.hidden));
    closeButton.addEventListener('click', () => setOpen(false));

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        const message = input.value.trim();
        if (!message || form.querySelector('button').disabled) {
            return;
        }

        addMessage(message, 'user');
        input.value = '';
        input.disabled = true;
        const sendButton = form.querySelector('button');
        sendButton.disabled = true;
        const pendingMessage = addMessage('Đang soạn câu trả lời...', 'bot');

        try {
            const headers = { 'Content-Type': 'application/json' };
            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }

            const response = await fetch('/api/chat', {
                method: 'POST',
                headers,
                credentials: 'same-origin',
                body: JSON.stringify({ message, history })
            });
            const result = await response.json().catch(() => ({}));
            if (!response.ok) {
                const details = {
                    401: 'Bạn cần đăng nhập lại rồi thử gửi tin nhắn.',
                    403: 'Phiên bảo mật đã hết hạn. Hãy tải lại trang rồi thử lại.',
                    404: 'Backend chatbot chưa được cập nhật. Hãy khởi động lại ứng dụng.',
                    429: 'Chatbot đã vượt giới hạn sử dụng. Vui lòng thử lại sau.',
                    502: 'Chatbot chưa kết nối được với Gemini. Kiểm tra API key, model, quota và log backend.'
                };
                throw new Error(
                    result.detail || result.message || result.title
                    || details[response.status]
                    || `Không gửi được tin nhắn (HTTP ${response.status}). Vui lòng thử lại sau.`
                );
            }

            pendingMessage.textContent = result.reply;
            history.push(
                { role: 'user', text: message },
                { role: 'model', text: result.reply }
            );
            if (history.length > 10) {
                history.splice(0, history.length - 10);
            }
        } catch (error) {
            pendingMessage.textContent = error.message || 'Không gửi được tin nhắn. Vui lòng thử lại.';
            pendingMessage.classList.add('shoptech-chat__bubble--error');
        } finally {
            input.disabled = false;
            sendButton.disabled = false;
            input.focus();
            messagesElement.scrollTop = messagesElement.scrollHeight;
        }
    });
})();
