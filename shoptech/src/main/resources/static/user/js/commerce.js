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
    let cartMutationPending = false;

    function element(tag, className, text) {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (text !== undefined) node.textContent = text;
        return node;
    }

    function icon(name) {
        const node = element('i', 'fa fa-' + name);
        node.setAttribute('aria-hidden', 'true');
        return node;
    }

    function setCartBusy(pending) {
        const layout = document.getElementById('cart-layout');
        if (layout) layout.setAttribute('aria-busy', String(pending));
        const box = document.getElementById('cart-content');
        if (box) box.querySelectorAll('button,input').forEach(control => {
            if (pending) {
                if (control.dataset.wasDisabled === undefined)
                    control.dataset.wasDisabled = String(control.disabled);
                control.disabled = true;
            } else if (control.dataset.wasDisabled !== undefined) {
                control.disabled = control.dataset.wasDisabled === 'true';
                delete control.dataset.wasDisabled;
            }
        });
        const checkout = document.getElementById('checkout-link');
        if (checkout) checkout.setAttribute('aria-disabled', String(pending));
    }

    async function mutateCart(item, method, quantity) {
        if (cartMutationPending) return;
        cartMutationPending = true;
        setCartBusy(true);
        try {
            const cart = await request(
                '/api/cart/items/' + item.id,
                method,
                method === 'PATCH' ? {quantity} : undefined
            );
            render(cart);
            notify(method === 'DELETE' ? 'Đã xóa sản phẩm khỏi giỏ hàng.' : 'Đã cập nhật số lượng.');
        } catch (error) {
            // Giữ lại số lượng đã được máy chủ xác nhận khi cập nhật thất bại.
            const row = document.querySelector('[data-cart-item-id="' + item.id + '"]');
            if (row) row.querySelector('input').value = item.quantity;
            notify(error.message, true);
        } finally {
            cartMutationPending = false;
            setCartBusy(false);
        }
    }

    function renderItem(item) {
        const row = element('article', 'cart-item');
        row.dataset.cartItemId = item.id;
        const url = '/products/' + encodeURIComponent(item.slug);
        const info = element('div', 'cart-item-info');
        const imageLink = element('a', 'cart-item-image');
        imageLink.href = url;
        imageLink.setAttribute('aria-label', item.name);
        imageLink.append(icon('picture-o'));
        if (item.image) {
            try {
                const imageUrl = new URL(item.image, window.location.href);
                if (imageUrl.protocol === 'https:' || imageUrl.protocol === 'http:') {
                    const image = document.createElement('img');
                    image.alt = item.name;
                    image.loading = 'lazy';
                    image.addEventListener('error', () => imageLink.replaceChildren(icon('picture-o')), {once: true});
                    image.src = imageUrl.href;
                    imageLink.replaceChildren(image);
                }
            } catch {
                // URL ảnh không hợp lệ: giữ biểu tượng thay thế.
            }
        }
        const description = element('div', 'cart-item-description');
        const name = element('h2', 'cart-item-name');
        const nameLink = element('a', '', item.name);
        nameLink.href = url;
        name.append(nameLink);
        const price = element('p', 'cart-item-price', 'Đơn giá: ' + money(item.price));
        let stockText = 'Còn hàng';
        if (item.stock <= 0) stockText = 'Tạm hết hàng';
        else if (item.quantity > item.stock) stockText = 'Chỉ còn ' + item.stock + ' sản phẩm';
        const stock = element('p', 'cart-item-stock', stockText);
        if (item.stock < item.quantity) stock.classList.add('is-warning');
        description.append(name, price, stock);
        info.append(imageLink, description);

        const quantity = element('div', 'cart-quantity');
        const minus = element('button', '', '−');
        minus.type = 'button';
        minus.setAttribute('aria-label', 'Giảm số lượng ' + item.name);
        minus.disabled = item.quantity <= 1 || item.stock <= 0;
        const input = document.createElement('input');
        input.type = 'number';
        input.required = true;
        input.min = '1';
        input.max = String(Math.max(1, item.stock));
        input.step = '1';
        input.value = item.quantity;
        input.disabled = item.stock <= 0;
        input.setAttribute('aria-label', 'Số lượng ' + item.name);
        const plus = element('button', '', '+');
        plus.type = 'button';
        plus.setAttribute('aria-label', 'Tăng số lượng ' + item.name);
        plus.disabled = item.quantity >= item.stock;

        function updateQuantity(value) {
            if (cartMutationPending) return;
            input.value = value;
            if (!input.reportValidity()) return;
            const requested = Number(input.value);
            if (requested !== item.quantity) mutateCart(item, 'PATCH', requested);
        }
        minus.addEventListener('click', () => updateQuantity(Math.min(item.stock, item.quantity - 1)));
        plus.addEventListener('click', () => updateQuantity(item.quantity + 1));
        input.addEventListener('change', () => updateQuantity(input.value));
        input.addEventListener('keydown', event => {
            if (event.key === 'Enter') {
                event.preventDefault();
                updateQuantity(input.value);
            }
        });
        quantity.append(minus, input, plus);

        const subtotal = element('div', 'cart-item-subtotal', money(item.subtotal));
        subtotal.setAttribute('aria-label', 'Thành tiền ' + money(item.subtotal));
        const remove = element('button', 'cart-remove');
        remove.type = 'button';
        remove.setAttribute('aria-label', 'Xóa ' + item.name + ' khỏi giỏ hàng');
        remove.title = 'Xóa sản phẩm';
        remove.append(icon('trash-o'));
        remove.addEventListener('click', () => mutateCart(item, 'DELETE'));
        row.append(info, quantity, subtotal, remove);
        return row;
    }

    function render(cart) {
        document.querySelectorAll('[data-cart-badge]').forEach(badge => badge.textContent = cart.quantity);
        const box = document.getElementById('cart-content');
        if (!box) return;
        box.replaceChildren();
        if (!cart.items.length) {
            const empty = element('div', 'cart-empty');
            empty.append(
                icon('shopping-cart'),
                element('h2', '', 'Giỏ hàng đang trống'),
                element('p', '', 'Hãy chọn thêm sản phẩm bạn yêu thích để tiếp tục mua sắm.')
            );
            box.append(empty);
        } else {
            cart.items.forEach(item => box.append(renderItem(item)));
        }
        const headings = document.getElementById('cart-column-headings');
        if (headings) headings.hidden = !cart.items.length;
        const description = document.getElementById('cart-description');
        if (description) description.textContent = cart.items.length
            ? cart.quantity + ' sản phẩm trong giỏ hàng'
            : 'Chưa có sản phẩm trong giỏ hàng';
        ['cart-subtotal', 'cart-total'].forEach(id => {
            const total = document.getElementById(id);
            if (total) total.textContent = money(cart.total);
        });
        const checkout = document.getElementById('checkout-link');
        if (checkout) checkout.hidden = !cart.items.length;
        const status = document.getElementById('cart-checkout-status');
        if (status) {
            status.hidden = !!cart.items.length;
            status.textContent = 'Thêm sản phẩm để tiến hành thanh toán.';
        }
        setCartBusy(cartMutationPending);
    }

    async function loadCart() {
        setCartBusy(true);
        try {
            render(await request('/api/cart'));
        } catch (error) {
            notify(error.message, true);
            const box = document.getElementById('cart-content');
            if (box) {
                const empty = element('div', 'cart-empty');
                const retry = element('button', 'cart-retry', 'Thử lại');
                retry.type = 'button';
                retry.addEventListener('click', loadCart);
                empty.append(
                    icon('exclamation-circle'),
                    element('h2', '', 'Chưa tải được giỏ hàng'),
                    element('p', '', error.message),
                    retry
                );
                box.replaceChildren(empty);
                const description = document.getElementById('cart-description');
                if (description) description.textContent = 'Vui lòng thử tải lại giỏ hàng.';
                const status = document.getElementById('cart-checkout-status');
                if (status) status.textContent = 'Chưa tải được giỏ hàng.';
            }
        } finally {
            setCartBusy(cartMutationPending);
        }
    }

    const checkoutLink = document.getElementById('checkout-link');
    if (checkoutLink) checkoutLink.addEventListener('click', event => {
        if (cartMutationPending) event.preventDefault();
    });
    document.addEventListener('click', async event => {
        const btn = event.target.closest('[data-add-product]'); if (!btn || btn.disabled) return;
        const input = document.getElementById('quantity'); if (input && !input.reportValidity()) return;
        btn.disabled = true;
        try { render(await request('/api/cart/items', 'POST', {productId: Number(btn.dataset.addProduct), quantity: input ? Number(input.value) : 1})); notify('Đã thêm vào giỏ hàng.'); }
        catch (error) { notify(error.message, true); } finally { btn.disabled = false; }
    });
    if (document.getElementById('cart-content') || document.querySelector('[data-cart-badge][data-authenticated="true"]'))
        loadCart();
    window.shoptechCommerce = {request, notify, money};
})();
