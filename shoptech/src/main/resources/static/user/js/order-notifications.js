(() => {
    'use strict';
    if (!document.querySelector('[data-order-notifications="true"]')) return;
    let socket, retry=1000, stopped=false;
    const labels={PENDING:'đang chờ xác nhận',CONFIRMED:'đã được xác nhận',PICKED_UP:'đã được lấy hàng',SHIPPING:'đang được giao',COMPLETED:'đã giao thành công',CANCELLED:'đã bị hủy',RETURN_REQUESTED:'đã có yêu cầu trả hàng',REFUNDED:'đã được hoàn tiền'};
    function connect() {
        if(stopped) return;
        socket=new WebSocket((location.protocol==='https:'?'wss:':'ws:')+'//'+location.host+'/ws/order-status');
        socket.onopen=()=>{retry=1000;};
        socket.onmessage=event=>{
            try {
                const data=JSON.parse(event.data);
                window.shoptechCommerce.notify('Đơn '+data.orderCode+' '+(labels[data.status] || data.status)+'.');
                window.dispatchEvent(new Event('shoptech:notifications-changed'));
                const status=document.getElementById('order-status');
                if(status && String(data.orderId)===status.dataset.orderId)status.textContent=data.status;
            }catch(ignored) {}
        };
        socket.onclose=()=>{if(!stopped){setTimeout(connect,retry);retry=Math.min(retry*2,30000);}};
        socket.onerror=()=>socket.close();
    }
    window.addEventListener('pagehide',()=>{stopped=true;if(socket)socket.close();});
    connect();
})();
