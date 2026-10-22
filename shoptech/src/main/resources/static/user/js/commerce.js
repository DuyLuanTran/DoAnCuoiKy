(() => {
    'use strict';
    const money = value => new Intl.NumberFormat('vi-VN', {style: 'currency', currency: 'VND'}).format(value);
    function notify(message, failed = false) {
        const box = document.getElementById('commerce-message');
        if (!box) return;
        box.hidden = false; box.className = 'alert ' + (failed ? 'alert-danger' : 'alert-success');
        box.textContent = message;
    }
    async function request(url, method = 'GET', body) {
        const headers = {Accept: 'application/json'};
        if (method !== 'GET') {
            headers['Content-Type'] = 'application/json';
            const token = document.querySelector('meta[name="_csrf"]');
            const header = document.querySelector('meta[name="_csrf_header"]');
            if (token && header) headers[header.content] = token.content;
        }
        const response = await fetch(url, {method, headers, credentials: 'same-origin', body: body === undefined ? undefined : JSON.stringify(body)});
        if (response.status === 401 || response.redirected) { window.location.assign('/login'); throw new Error('Vui lòng đăng nhập.'); }
        const data = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(data.detail || 'Không thể xử lý yêu cầu. Vui lòng thử lại.');
        return data;
    }
    function render(cart) {
        document.querySelectorAll('[data-cart-badge]').forEach(badge => badge.textContent = cart.quantity);
        const box = document.getElementById('cart-content');
        if (!box) return;
        box.replaceChildren();
        if (!cart.items.length) { box.textContent = 'Giỏ hàng trống. Hãy chọn sản phẩm để tiếp tục.'; }
        else {
            const responsive = document.createElement('div'); responsive.className = 'table-responsive';
            const table = document.createElement('table'); table.className = 'table';
            const head = table.createTHead().insertRow();
            ['Sản phẩm', 'Đơn giá', 'Số lượng', 'Thành tiền', 'Thao tác'].forEach(text => { const th = document.createElement('th'); th.textContent = text; head.append(th); });
            const rows = table.createTBody();
            cart.items.forEach(item => {
                const row = rows.insertRow(); const link = document.createElement('a');
                link.href = '/products/' + encodeURIComponent(item.slug); link.textContent = item.name; row.insertCell().append(link);
                row.insertCell().textContent = money(item.price);
                const qty = document.createElement('input'); qty.type = 'number'; qty.min = '1'; qty.step = '1'; qty.max = Math.max(1, item.stock); qty.value = item.quantity;
                qty.setAttribute('aria-label', 'Số lượng ' + item.name); row.insertCell().append(qty);
                row.insertCell().textContent = money(item.subtotal);
                const actions = row.insertCell();
                [['Cập nhật', 'PATCH'], ['Xóa', 'DELETE']].forEach(([text, method]) => {
                    const btn = document.createElement('button'); btn.className = 'btn btn-default'; btn.textContent = text;
                    btn.addEventListener('click', async () => {
                        if (method === 'PATCH' && !qty.reportValidity()) return;
                        row.querySelectorAll('button,input').forEach(el => el.disabled = true);
                        try { render(await request('/api/cart/items/' + item.id, method, method === 'PATCH' ? {quantity: Number(qty.value)} : undefined)); notify('Đã cập nhật giỏ hàng.'); }
                        catch (error) { notify(error.message, true); }
                        finally { row.querySelectorAll('button,input').forEach(el => el.disabled = false); }
                    }); actions.append(btn);
                });
            });
            responsive.append(table); box.append(responsive);
            const total = document.createElement('h3'); total.textContent = 'Tổng tiền: ' + money(cart.total); box.append(total);
        }
        const checkout = document.getElementById('checkout-link'); if (checkout) checkout.hidden = !cart.items.length;
    }
    document.addEventListener('click', async event => {
        const btn = event.target.closest('[data-add-product]'); if (!btn || btn.disabled) return;
        const input = document.getElementById('quantity'); if (input && !input.reportValidity()) return;
        btn.disabled = true;
        try { render(await request('/api/cart/items', 'POST', {productId: Number(btn.dataset.addProduct), quantity: input ? Number(input.value) : 1})); notify('Đã thêm vào giỏ hàng.'); }
        catch (error) { notify(error.message, true); } finally { btn.disabled = false; }
    });
    if (document.getElementById('cart-content') || document.querySelector('[data-cart-badge][data-authenticated="true"]'))
        request('/api/cart').then(render).catch(error => notify(error.message, true));
    window.shoptechCommerce = {request, notify, money};
})();
