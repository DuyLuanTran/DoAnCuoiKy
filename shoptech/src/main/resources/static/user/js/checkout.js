(() => {
    const {request, notify, money} = window.shoptechCommerce;
    const summary = document.getElementById('checkout-summary'), form = document.getElementById('checkout-form');
    const submit = document.getElementById('place-order');
    // Keep the same key after network failure/reload so retry cannot place a second order.
    const storageKey = 'shoptech-checkout-key:' + document.querySelector('meta[name="commerce-user"]').content;
    let key = sessionStorage.getItem(storageKey);
    if (!key) { key = crypto.randomUUID(); sessionStorage.setItem(storageKey,key); }
    request('/api/cart').then(cart => {
        summary.replaceChildren();
        cart.items.forEach(item => { const p = document.createElement('p'); p.textContent = item.quantity + ' × ' + item.name + ': ' + money(item.subtotal); summary.append(p); });
        const total = document.createElement('strong'); total.textContent = 'Tổng thanh toán: ' + money(cart.total); summary.append(total);
        if (!cart.items.length) notify('Giỏ hàng trống. Vui lòng thêm sản phẩm.',true);
        submit.disabled = !cart.items.length;
    }).catch(error => { summary.textContent = 'Không thể tải giỏ hàng.'; notify(error.message,true); });
    let pending = false;
    form.addEventListener('submit', async event => {
        event.preventDefault(); if (pending || !form.reportValidity()) return;
        pending = true; submit.disabled = true;
        const input = Object.fromEntries(new FormData(form)); input.requestKey = key;
        try {
            const order = await request('/api/orders','POST',input);
            sessionStorage.removeItem(storageKey); window.location.assign('/orders/' + order.id);
        } catch (error) { notify(error.message,true); pending = false; submit.disabled = false; }
    });
})();
