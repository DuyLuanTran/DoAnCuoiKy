(() => {
 const form=document.getElementById('order-status-form');if(!form)return;
 form.addEventListener('submit',async event=>{
  event.preventDefault();const button=form.querySelector('button'),message=document.getElementById('status-message');if(button.disabled)return;
  const status=form.elements.status.value,reason=form.elements.reason;
  if(['CANCELLED','RETURN_REQUESTED','REFUNDED'].includes(status)&&!reason.value.trim()){
   message.textContent=status==='REFUNDED'?'Vui lòng nhập lý do hoàn tiền.':'Vui lòng nhập lý do hủy hoặc trả hàng.';
   message.classList.add('text-danger');reason.focus();return;
  }
  button.disabled=true;message.classList.remove('text-danger');message.textContent='Đang cập nhật…';
  try{const headers={'Content-Type':'application/json',Accept:'application/json'};headers[document.querySelector('meta[name="_csrf_header"]').content]=document.querySelector('meta[name="_csrf"]').content;
   const response=await fetch(form.dataset.url,{method:'PATCH',headers,credentials:'same-origin',body:JSON.stringify({expectedStatus:form.dataset.expected,status:form.elements.status.value,reason:form.elements.reason.value})});
   if(response.redirected){location.assign('/login');return;}const data=await response.json().catch(()=>({}));if(!response.ok)throw new Error(data.detail||'Không thể cập nhật. Hãy tải lại trang.');location.reload();
  }catch(error){message.textContent=error.message;message.classList.add('text-danger');button.disabled=false;}
 });
})();
