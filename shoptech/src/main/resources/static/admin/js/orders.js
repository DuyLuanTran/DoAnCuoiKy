(() => {
    const form = document.getElementById('order-status-form'); if (!form) return;
    let pending = false;
    form.addEventListener('submit', async event => {
        event.preventDefault(); if (pending) return; pending = true;
        const button = form.querySelector('button'); button.disabled = true;
        try {
            await window.shoptechCommerce.request('/admin/orders/' + form.dataset.orderId + '/status','PATCH', {expectedStatus: form.dataset.expected, status: form.elements.status.value});
            window.location.reload();
        } catch (error) { window.shoptechCommerce.notify(error.message,true); pending = false; button.disabled = false; }
    });
})();
