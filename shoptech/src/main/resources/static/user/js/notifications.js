(() => {
    'use strict';
    const root = document.querySelector('[data-notification-root]');
    if (!root) return;
    const toggle = root.querySelector('[data-notification-toggle]');
    const panel = root.querySelector('[data-notification-panel]');
    const count = root.querySelector('[data-notification-count]');
    const csrf = document.querySelector('meta[name="_csrf"]')?.content;

    async function refresh(page = 0) {
        try {
            const response = await fetch('/notifications?page=' + page, {credentials: 'same-origin'});
            if (!response.ok) return;
            const inbox = await response.json();
            count.textContent = inbox.unread > 99 ? '99+' : inbox.unread;
            count.hidden = inbox.unread === 0;
            if (page === 0) {
                panel.replaceChildren();
                const title = document.createElement('h3');
                title.textContent = 'Thông báo';
                panel.append(title);
            } else {
                panel.querySelector('[data-notification-more]')?.remove();
            }
            if (page === 0 && !inbox.items.length) {
                const empty = document.createElement('p');
                empty.textContent = 'Chưa có thông báo.';
                panel.append(empty);
            }
            for (const item of inbox.items) {
                const form = document.createElement('form');
                form.method = 'post';
                form.action = item.url;
                if (csrf) {
                    const token = document.createElement('input');
                    token.type = 'hidden'; token.name = '_csrf'; token.value = csrf;
                    form.append(token);
                }
                const button = document.createElement('button');
                button.type = 'submit';
                button.className = 'notification-item' + (item.read ? '' : ' unread');
                button.textContent = item.message;
                const time = document.createElement('small');
                time.textContent = new Date(item.createdAt).toLocaleString('vi-VN');
                button.append(time); form.append(button); panel.append(form);
            }
            if (inbox.hasMore) {
                const more = document.createElement('button');
                more.type = 'button'; more.className = 'notification-more';
                more.dataset.notificationMore = '';
                more.textContent = 'Xem thêm thông báo';
                more.addEventListener('click', () => refresh(page + 1));
                panel.append(more);
            }
        } catch (_) { /* Kết nối tạm thời gián đoạn; thử lại ở lần mở tiếp theo. */ }
    }
    toggle.addEventListener('click', () => {
        const opening = panel.hidden;
        panel.hidden = !opening;
        toggle.setAttribute('aria-expanded', String(opening));
        if (opening) refresh();
    });
    document.addEventListener('click', event => {
        if (!root.contains(event.target)) {
            panel.hidden = true;
            toggle.setAttribute('aria-expanded', 'false');
        }
    });
    refresh();
    window.addEventListener('shoptech:notifications-changed', () => refresh());
    window.setInterval(() => { if (panel.hidden) refresh(); }, 30000);
})();
