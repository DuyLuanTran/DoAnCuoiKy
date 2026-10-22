(() => {
    const form=document.getElementById('revenue-filter'), message=document.getElementById('revenue-message');
    const {request,money}=window.shoptechCommerce; let chart;
    async function load() {
        const button=form.querySelector('button'); button.disabled=true; message.textContent='Đang tải…';
        try {
            const report=await request('/admin/revenue/data?'+new URLSearchParams(new FormData(form)));
            document.getElementById('revenue-total').textContent='Doanh thu: '+money(report.totalRevenue);
            document.getElementById('revenue-count').textContent='Số đơn hoàn tất: '+report.completedOrders;
            message.textContent=report.completedOrders?'':'Không có đơn hoàn tất trong khoảng thời gian này.';
            const table=document.getElementById('revenue-table');table.replaceChildren();
            report.points.forEach(point=>{const row=table.insertRow();row.insertCell().textContent=point.label;row.insertCell().textContent=money(point.revenue);row.insertCell().textContent=point.orders;});
            if(chart)chart.destroy();
            if(window.Chart) chart=new Chart(document.getElementById('revenue-chart'),{type:'bar',data:{labels:report.points.map(p=>p.label),datasets:[{label:'Doanh thu (VND)',data:report.points.map(p=>p.revenue),backgroundColor:'#4e73df'}]},options:{responsive:true,scales:{yAxes:[{ticks:{beginAtZero:true,callback:money}}]},tooltips:{callbacks:{label:item=>money(item.yLabel)}}}});
        }catch(error){message.textContent=error.message;}finally{button.disabled=false;}
    }
    form.addEventListener('submit',event=>{event.preventDefault();if(form.reportValidity())load();});load();
})();
