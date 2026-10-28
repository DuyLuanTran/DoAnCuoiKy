(() => {
'use strict';
const qs = (s, root=document) => root.querySelector(s);
const all = (s, root=document) => [...root.querySelectorAll(s)];
const authenticated = () => {const name=qs('meta[name="commerce-user"]')?.content;return !!name && name !== 'anonymousUser';};
function el(tag, cls, text) {const node=document.createElement(tag);if(cls)node.className=cls;if(text!==undefined)node.textContent=text;return node;}
function message(text, failed=false, target=qs('[data-review-message]') || qs('[data-review-admin-message]') || qs('#commerce-message')) {if(!target)return;target.hidden=false;target.textContent=text;target.classList.toggle('community-error',failed);}
async function request(url, method='GET', body) {
 const headers={Accept:'application/json'};
 if(method!=='GET'){const token=qs('meta[name="_csrf"]'),header=qs('meta[name="_csrf_header"]');if(token&&header)headers[header.content]=token.content;}
 if(body && !(body instanceof FormData)){headers['Content-Type']='application/json';body=JSON.stringify(body);}
 const response=await fetch(url,{method,body,headers,credentials:'same-origin'});
 if(response.status===401||response.redirected){location.assign('/login');throw new Error('Vui lòng đăng nhập.');}
 const data=await response.json().catch(()=>({}));if(!response.ok)throw new Error(data.detail || 'Không thể xử lý yêu cầu. Vui lòng thử lại.');return data;
}
function setFavorite(button,selected){button.setAttribute('aria-pressed',String(selected));button.setAttribute('aria-label',selected?'Bỏ yêu thích':'Thêm vào yêu thích');const icon=qs('i',button);if(icon)icon.className='fa '+(selected?'fa-heart':'fa-heart-o');}
const favorites=all('[data-favorite]');
const favoriteIds=[...new Set(favorites.map(b=>b.dataset.favorite))];
if(authenticated()&&favoriteIds.length)request('/api/favorites/selected?'+new URLSearchParams({ids:favoriteIds.join(',')})).then(ids=>favorites.forEach(b=>setFavorite(b,ids.includes(Number(b.dataset.favorite))))).catch(e=>message(e.message,true));
favorites.forEach(button=>button.addEventListener('click',async()=>{
 if(!authenticated()){location.assign('/login');return;}button.disabled=true;
 try {const selected=button.getAttribute('aria-pressed')!=='true';await request('/api/favorites/'+button.dataset.favorite,selected?'PUT':'DELETE');favorites.filter(b=>b.dataset.favorite===button.dataset.favorite).forEach(b=>setFavorite(b,selected));message(selected?'Đã thêm vào yêu thích.':'Đã bỏ yêu thích.');if(location.pathname==='/favorites'&&!selected)location.reload();}
 catch(e){message(e.message,true);}finally{button.disabled=false;}
}));
const ratingNodes=all('[data-rating-product]');const ratingIds=[...new Set(ratingNodes.map(n=>n.dataset.ratingProduct))];
if(ratingIds.length)request('/api/community/summaries?'+new URLSearchParams({ids:ratingIds.join(',')})).then(data=>ratingNodes.forEach(n=>{const s=data[n.dataset.ratingProduct];if(s?.count)n.textContent='★ '+s.average.toFixed(1)+' / 5 · '+s.count+' đánh giá';})).catch(()=>{});
function attachment(file){
 if(file.kind==='VIDEO'){const video=el('video');video.src=file.url;video.controls=true;video.preload='metadata';return video;}
 const link=el('a');link.href=file.url;link.target='_blank';link.rel='noopener';const image=el('img');image.src=file.url;image.alt='Ảnh đính kèm đánh giá';image.loading='lazy';link.append(image);return link;
}
const panel=qs('[data-reviews-product]');
if(panel){
 const id=panel.dataset.reviewsProduct,form=qs('[data-review-form]',panel),info=qs('[data-review-message]',panel);let currentPage=0;
 const preview=qs('[data-review-preview]',panel);let previews=[];
 qs('#review-files',panel).addEventListener('change',event=>{previews.forEach(URL.revokeObjectURL);previews=[];preview.replaceChildren();const files=[...event.target.files];if(files.filter(f=>f.type.startsWith('image/')).length>3||files.filter(f=>f.type.startsWith('video/')).length>1||files.some(f=>f.size>(f.type.startsWith('video/')?20:5)*1024*1024)||files.reduce((n,f)=>n+f.size,0)>25*1024*1024){event.target.value='';message('Chọn tối đa 3 ảnh và 1 video, trong giới hạn dung lượng.',true,info);return;}files.forEach(file=>{const url=URL.createObjectURL(file);previews.push(url);preview.append(attachment({url,kind:file.type.startsWith('video/')?'VIDEO':'IMAGE'}));});});
 window.addEventListener('pagehide',()=>previews.forEach(URL.revokeObjectURL));
 async function eligibility(){if(!authenticated()){info.replaceChildren(document.createTextNode('Đăng nhập để yêu thích và đánh giá sản phẩm. '));const a=el('a','','Đăng nhập');a.href='/login';info.append(a);return;}const state=await request('/api/reviews/eligibility/'+id);form.hidden=!state.allowed;message(state.message,false,info);}
 async function load(page=0){
  const data=await request('/api/community/products/'+id+'/reviews?page='+page);currentPage=data.page;
  qs('[data-review-summary]',panel).textContent=data.total?data.summary.average.toFixed(1)+' / 5 sao · '+data.total+' đánh giá từ khách đã mua':'Chưa có đánh giá. Hãy là người đầu tiên chia sẻ trải nghiệm.';
  const list=qs('[data-review-list]',panel);list.replaceChildren();
  data.items.forEach(review=>{const article=el('article','review-entry'),head=el('div','review-entry-header');head.append(el('strong','',review.author),el('span','review-stars','★'.repeat(review.rating)+'☆'.repeat(5-review.rating)));const date=el('time','',new Date(review.createdAt).toLocaleDateString('vi-VN'));date.dateTime=review.createdAt;head.append(date);article.append(head,el('p','review-text',review.content));if(review.media.length){const media=el('div','review-media');review.media.forEach(file=>media.append(attachment(file)));article.append(media);}if(review.sellerReply){const reply=el('div','review-reply');reply.append(el('strong','','Phản hồi của shop'),el('p','',review.sellerReply));article.append(reply);}list.append(article);});
  const paging=qs('[data-review-paging]',panel);paging.replaceChildren();if(data.pages>1){const prev=el('button','','Trang trước'),next=el('button','','Trang sau');prev.type=next.type='button';prev.disabled=data.page===0;next.disabled=data.page>=data.pages-1;prev.onclick=()=>load(currentPage-1).catch(e=>message(e.message,true,info));next.onclick=()=>load(currentPage+1).catch(e=>message(e.message,true,info));paging.append(prev,el('span','','Trang '+(data.page+1)+' / '+data.pages),next);}
 }
 form.addEventListener('submit',async event=>{event.preventDefault();const button=qs('button[type="submit"]',form);button.disabled=true;try{await request('/api/reviews/products/'+id,'POST',new FormData(form));form.reset();previews.forEach(URL.revokeObjectURL);previews=[];preview.replaceChildren();form.hidden=true;await load();message('Đã gửi đánh giá. Cảm ơn bạn đã chia sẻ!',false,info);}catch(e){message(e.message,true,info);}finally{button.disabled=false;}});
 load().catch(e=>message(e.message,true,info));eligibility().catch(e=>message(e.message,true,info));
}
all('[data-review-action]').forEach(form=>form.addEventListener('submit',async event=>{
 event.preventDefault();const action=form.dataset.reviewAction,button=qs('button',form);button.disabled=true;
 try {const body={version:Number(form.dataset.version)};if(action==='reply')body.reply=form.elements.reply.value;else{body.reason=form.elements.reason.value;body.hidden=form.dataset.hidden==='true';}await request('/'+(action==='reply'?'seller':'admin')+'/reviews/'+form.dataset.id+'/'+action,'PATCH',body);location.reload();}
 catch(e){message(e.message,true);}finally{button.disabled=false;}
}));
})();
