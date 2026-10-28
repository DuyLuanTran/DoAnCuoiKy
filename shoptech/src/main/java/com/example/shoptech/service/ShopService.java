package com.example.shoptech.service;

import com.example.shoptech.dto.ShopForm;
import com.example.shoptech.entity.*;
import com.example.shoptech.repository.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.*;

@Service @Transactional(readOnly=true)
public class ShopService {
    private final ShopRepository shops;
    private final UserRepository users;
    private final RoleRepository roles;
    private final MarketplaceSettingsRepository settings;
    private final ShopReviewHistoryRepository history;
    public ShopService(ShopRepository shops,UserRepository users,RoleRepository roles,MarketplaceSettingsRepository settings,ShopReviewHistoryRepository history){
        this.shops=shops;this.users=users;this.roles=roles;this.settings=settings;this.history=history;
    }
    public Optional<Shop> own(String username){return shops.findByOwnerUsername(username);}
    public Shop requiredOwn(String username){return own(username).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Bạn chưa đăng ký shop."));}
    @Transactional @PreAuthorize("hasRole('SELLER')")
    public Shop lockOwnActive(String username){
        activeUser(username);
        Shop shop=shops.lockById(requiredOwn(username).getId()).orElseThrow();
        if(shop.getStatus()!=ShopStatus.ACTIVE)throw error(HttpStatus.FORBIDDEN,"Shop đang bị khóa hoặc chưa được duyệt.");
        return shop;
    }
    public BigDecimal defaultRate(){return settings.findById(1).orElseThrow(()->error(HttpStatus.SERVICE_UNAVAILABLE,"Chưa cấu hình sàn.")).getDefaultCommissionRate();}
    public BigDecimal effectiveRate(Shop shop){return shop.getCommissionRate()==null?defaultRate():shop.getCommissionRate();}
    public ShopForm form(Shop shop){ShopForm f=new ShopForm();f.setName(shop.getName());f.setEmail(shop.getEmail());f.setPhone(shop.getPhone());f.setAddress(shop.getAddress());f.setDescription(shop.getDescription());return f;}
    @Transactional @PreAuthorize("isAuthenticated()")
    public void register(String username,ShopForm form){
        User owner=activeUser(username);
        Shop shop=own(username).orElse(null);
        boolean fresh=shop==null;
        if(fresh){shop=new Shop();shop.setOwner(owner);shop.setSlug(slug(form.getName())+"-"+owner.getId());}
        else{shop=shops.lockById(shop.getId()).orElseThrow();if(shop.getStatus()!=ShopStatus.REJECTED)throw error(HttpStatus.CONFLICT,"Bạn đã có hồ sơ đăng ký shop.");}
        ShopStatus previous=shop.getStatus();
        copy(form,shop);shop.setStatus(ShopStatus.PENDING);shop.setSubmittedAt(OffsetDateTime.now());
        shop.setReviewedBy(null);shop.setReviewedAt(null);shop.setRejectionReason(null);
        shops.saveAndFlush(shop);
        if(!fresh)record(shop,owner,previous,ShopStatus.PENDING,"Gửi lại hồ sơ đăng ký");
    }
    @Transactional @PreAuthorize("hasRole('SELLER')")
    public void updateOwn(String username,ShopForm form){
        activeUser(username);
        Shop shop=shops.lockById(requiredOwn(username).getId()).orElseThrow();
        if(shop.getStatus()!=ShopStatus.ACTIVE)throw error(HttpStatus.FORBIDDEN,"Shop chưa hoạt động hoặc đang bị khóa.");
        copy(form,shop);shops.flush();
    }
    @PreAuthorize("hasRole('ADMIN')")
    public Page<Shop> search(String keyword,ShopStatus status,int page){
        if(keyword.length()>100)throw error(HttpStatus.BAD_REQUEST,"Từ khóa tối đa 100 ký tự.");
        return shops.search(keyword.strip(),status,PageRequest.of(Math.max(0,page),10,Sort.by("createdAt").descending().and(Sort.by("id").descending())));
    }
    @PreAuthorize("hasRole('ADMIN')") public Map<String,Long> statistics(){Map<String,Long> result=new LinkedHashMap<>();for(ShopStatus s:ShopStatus.values())result.put(s.name(),shops.countByStatus(s));return result;}
    @PreAuthorize("hasRole('ADMIN')") public Shop get(Long id){return shops.findById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Không tìm thấy shop."));}
    @PreAuthorize("hasRole('ADMIN')") public List<Shop> adminOptions(){return shops.findAll(Sort.by("name").ascending());}
    @PreAuthorize("hasRole('ADMIN')") public List<ShopReviewHistory> history(Long id){get(id);return history.findByShopIdOrderByCreatedAtDescIdDesc(id);}
    @Transactional @PreAuthorize("hasRole('ADMIN')")
    public void review(String admin,Long id,String action,ShopStatus expected,String reason){
        Shop initial=get(id);
        User owner=activeUser(initial.getOwner().getUsername());
        Shop shop=shops.lockById(id).orElseThrow();
        if(shop.getStatus()!=expected)throw error(HttpStatus.CONFLICT,"Hồ sơ đã được cập nhật. Hãy tải lại trang.");
        ShopStatus next=switch(action){
            case "approve" -> {require(shop,ShopStatus.PENDING);yield ShopStatus.ACTIVE;}
            case "reject" -> {require(shop,ShopStatus.PENDING);yield ShopStatus.REJECTED;}
            case "suspend" -> {require(shop,ShopStatus.ACTIVE);yield ShopStatus.SUSPENDED;}
            case "resume" -> {require(shop,ShopStatus.SUSPENDED);yield ShopStatus.ACTIVE;}
            default -> throw error(HttpStatus.BAD_REQUEST,"Thao tác không hợp lệ.");
        };
        String note=reason==null?"":reason.strip();
        if(note.length()>1000)throw error(HttpStatus.BAD_REQUEST,"Lý do tối đa 1000 ký tự.");
        if((next==ShopStatus.REJECTED||next==ShopStatus.SUSPENDED)&&note.isEmpty())throw error(HttpStatus.BAD_REQUEST,"Vui lòng nhập lý do.");
        User actor=users.findByUsernameAndIsDeletedFalse(admin).orElseThrow();
        ShopStatus previous=shop.getStatus();shop.setStatus(next);shop.setReviewedBy(actor);shop.setReviewedAt(OffsetDateTime.now());
        if(next==ShopStatus.REJECTED)shop.setRejectionReason(note);
        if(next==ShopStatus.SUSPENDED)shop.setSuspensionReason(note);
        if(action.equals("resume"))shop.setSuspensionReason(null);
        if(next==ShopStatus.ACTIVE)owner.getRoles().add(roles.findByName("ROLE_SELLER").orElseThrow());
        record(shop,actor,previous,next,note);shops.flush();
    }
    @Transactional @PreAuthorize("hasRole('ADMIN')")
    public void setDefaultRate(String admin,BigDecimal rate){validateRate(rate);MarketplaceSettings s=settings.findById(1).orElseThrow();s.setDefaultCommissionRate(rate);s.setUpdatedBy(users.findByUsernameAndIsDeletedFalse(admin).orElseThrow());settings.flush();}
    @Transactional @PreAuthorize("hasRole('ADMIN')")
    public void setShopRate(String admin,Long id,BigDecimal rate){if(rate!=null)validateRate(rate);Shop s=shops.lockById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Không tìm thấy shop."));s.setCommissionRate(rate);shops.flush();}
    private void validateRate(BigDecimal rate){if(rate==null||rate.signum()<0||rate.compareTo(new BigDecimal("100"))>0||rate.scale()>2)throw error(HttpStatus.BAD_REQUEST,"Chiết khấu từ 0 đến 100%, tối đa 2 chữ số thập phân.");}
    private User activeUser(String username){return users.lockActiveByUsername(username).orElseThrow(()->error(HttpStatus.FORBIDDEN,"Tài khoản không hoạt động."));}
    private void require(Shop shop,ShopStatus state){if(shop.getStatus()!=state)throw error(HttpStatus.CONFLICT,"Trạng thái shop không cho phép thao tác này.");}
    private void record(Shop shop,User actor,ShopStatus previous,ShopStatus next,String reason){ShopReviewHistory h=new ShopReviewHistory();h.setShop(shop);h.setActor(actor);h.setPreviousStatus(previous);h.setNewStatus(next);h.setReason(reason);history.save(h);}
    private void copy(ShopForm f,Shop s){s.setName(f.getName().strip());s.setEmail(f.getEmail().strip());s.setPhone(f.getPhone().strip());s.setAddress(f.getAddress().strip());s.setDescription(f.getDescription()==null?null:f.getDescription().strip());}
    private String slug(String value){String text=Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).replace('đ','d').replaceAll("[^a-z0-9]+","-").replaceAll("^-|-$","");return text.isEmpty()?"shop":text;}
    private static ResponseStatusException error(HttpStatus status,String message){return new ResponseStatusException(status,message);}
}
