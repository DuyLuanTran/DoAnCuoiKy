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
        if (cart.items.length) {
            const heading = document.createElement('div');
            heading.className = 'checkout-summary-head';
            const productLabel = document.createElement('span'); productLabel.textContent = 'Sản phẩm';
            const priceLabel = document.createElement('span'); priceLabel.textContent = 'Thành tiền';
            heading.append(productLabel, priceLabel); summary.append(heading);
        }
        cart.items.forEach(item => {
            const row = document.createElement('div'); row.className = 'checkout-summary-item';
            const description = document.createElement('div'); description.className = 'checkout-summary-name';
            const name = document.createElement('p'); name.textContent = item.name;
            const quantity = document.createElement('small'); quantity.textContent = 'Số lượng: ' + item.quantity;
            description.append(name, quantity);
            const price = document.createElement('span'); price.className = 'checkout-summary-price'; price.textContent = money(item.subtotal);
            row.append(description, price); summary.append(row);
        });
        const total = document.createElement('div'); total.className = 'checkout-summary-total';
        const totalLabel = document.createElement('span'); totalLabel.textContent = 'Tổng thanh toán';
        const totalAmount = document.createElement('strong'); totalAmount.textContent = money(cart.total);
        total.append(totalLabel, totalAmount); summary.append(total);
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
