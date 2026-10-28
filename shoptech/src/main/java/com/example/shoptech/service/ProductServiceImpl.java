package com.example.shoptech.service;

import com.example.shoptech.dto.ProductForm;
import com.example.shoptech.entity.*;
import com.example.shoptech.repository.*;
import jakarta.validation.Validator;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.*;

@Service @Transactional(readOnly=true)
public class ProductServiceImpl implements ProductService {
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final CloudinaryService images;
    private final Validator validator;
    private final ShopService shops;
    private final UserRepository users;
    private final ProductModerationHistoryRepository history;
    public ProductServiceImpl(ProductRepository products,CategoryRepository categories,CloudinaryService images,Validator validator,ShopService shops,UserRepository users,ProductModerationHistoryRepository history){
        this.products=products;this.categories=categories;this.images=images;this.validator=validator;this.shops=shops;this.users=users;this.history=history;
    }
    @Override @PreAuthorize("hasRole('ADMIN')")
    public Page<Product> searchAdmin(String keyword,Long categoryId,Long shopId,Pageable page){return products.searchMarketplace(keyword.strip(),categoryId,shopId,false,page);}
    @Override @PreAuthorize("hasRole('ADMIN')")
    public Product getById(Long id){return products.findByIdAndIsDeletedFalse(id).orElseThrow(()->notFound());}
    @Override @PreAuthorize("hasRole('ADMIN')")
    public List<ProductModerationHistory> moderationHistory(Long id){getById(id);return history.findByProductIdOrderByCreatedAtDescIdDesc(id);}
    @Override @Transactional @PreAuthorize("hasRole('ADMIN')")
    public void moderate(String username,Long id,boolean blocked,boolean expectedBlocked,String reason){
        Product p=products.lockById(id).filter(x->!Boolean.TRUE.equals(x.getIsDeleted())).orElseThrow(()->notFound());
        if(Boolean.TRUE.equals(p.getIsBlocked())!=expectedBlocked)throw new ResponseStatusException(HttpStatus.CONFLICT,"Sản phẩm đã được cập nhật. Hãy tải lại trang.");
        String note=reason==null?"":reason.strip();
        if(note.isEmpty()||note.length()>1000)throw new IllegalArgumentException("Nhập lý do từ 1 đến 1000 ký tự.");
        User actor=users.findByUsernameAndIsDeletedFalse(username).orElseThrow();
        p.setIsBlocked(blocked);p.setModerationReason(blocked?note:null);p.setModeratedBy(actor.getId());p.setModeratedAt(OffsetDateTime.now());
        ProductModerationHistory event=new ProductModerationHistory();event.setProduct(p);event.setActor(actor);event.setBlocked(blocked);event.setReason(note);history.save(event);products.flush();
    }
    @Override @PreAuthorize("hasRole('SELLER')")
    public Page<Product> searchSeller(String username,String keyword,Long categoryId,boolean trash,Pageable page){return products.searchMarketplace(keyword.strip(),categoryId,shops.requiredOwn(username).getId(),trash,page);}
    @Override @PreAuthorize("hasRole('SELLER')")
    public Product getSellerProduct(String username,Long id){return products.findByIdAndShopId(id,shops.requiredOwn(username).getId()).orElseThrow(()->notFound());}
    @Override @PreAuthorize("hasRole('SELLER')")
    public ProductForm getSellerForm(String username,Long id){Product p=getSellerProduct(username,id);if(Boolean.TRUE.equals(p.getIsDeleted()))throw notFound();return toForm(p);}
    @Override @Transactional @PreAuthorize("hasRole('SELLER')")
    public void createSeller(String username,ProductForm form){
        Shop shop=shops.lockOwnActive(username);validate(form);validateSku(form.getSku(),shop.getId(),null);
        Category category=category(form.getCategoryId(),null);
        if(form.getImageFile()==null||form.getImageFile().isEmpty())throw new IllegalArgumentException("Vui lòng chọn ảnh chính cho sản phẩm.");
        images.validateImage(form.getImageFile());
        Product p=new Product();p.setShop(shop);apply(p,form,category);p.setSlug(slug(form.getName()));
        p.setMainImage(images.uploadImage(form.getImageFile()).secureUrl());products.saveAndFlush(p);
    }
    @Override @Transactional @PreAuthorize("hasRole('SELLER')")
    public void updateSeller(String username,Long id,ProductForm form){
        Shop shop=shops.lockOwnActive(username);Product p=lockedOwn(shop,id,false);validate(form);validateSku(form.getSku(),shop.getId(),id);
        Category category=category(form.getCategoryId(),p.getCategory().getId());
        if(form.getImageFile()!=null&&!form.getImageFile().isEmpty()){images.validateImage(form.getImageFile());p.setMainImage(images.uploadImage(form.getImageFile()).secureUrl());}
        apply(p,form,category);products.flush();
    }
    @Override @Transactional @PreAuthorize("hasRole('SELLER')")
    public void trashSeller(String username,Long id){Product p=lockedOwn(shops.lockOwnActive(username),id,false);p.setIsDeleted(true);p.setDeletedAt(OffsetDateTime.now());products.flush();}
    @Override @Transactional @PreAuthorize("hasRole('SELLER')")
    public void restoreSeller(String username,Long id){Product p=lockedOwn(shops.lockOwnActive(username),id,true);p.setIsDeleted(false);p.setDeletedAt(null);products.flush();}
    private Product lockedOwn(Shop shop,Long id,boolean deleted){
        Product p=products.lockById(id).orElseThrow(()->notFound());
        if(!p.getShop().getId().equals(shop.getId())||Boolean.TRUE.equals(p.getIsDeleted())!=deleted)throw notFound();return p;
    }
    private Category category(Long id,Long current){Category c=categories.findByIdAndIsDeletedFalse(id).orElseThrow(()->new IllegalArgumentException("Danh mục không tồn tại hoặc đã bị xóa."));if(!Boolean.TRUE.equals(c.getIsActive())&&!Objects.equals(id,current))throw new IllegalArgumentException("Vui lòng chọn danh mục đang bật.");return c;}
    private void validate(ProductForm f){if(f==null)throw new IllegalArgumentException("Dữ liệu sản phẩm không hợp lệ.");String message=validator.validate(f).stream().map(v->v.getMessage()).sorted().findFirst().orElse(null);if(message!=null)throw new IllegalArgumentException(message);}
    private void validateSku(String sku,Long shopId,Long id){if(sku!=null&&(id==null?products.existsByShopIdAndSku(shopId,sku):products.existsByShopIdAndSkuAndIdNot(shopId,sku,id)))throw new IllegalArgumentException("Mã sản phẩm đã tồn tại trong shop, kể cả sản phẩm trong thùng rác.");}
    private void apply(Product p,ProductForm f,Category c){p.setCategory(c);p.setName(f.getName());p.setSku(f.getSku());p.setDescription(f.getDescription());p.setSpecifications(f.getSpecifications());p.setPrice(f.getPrice());p.setDiscountPrice(f.getDiscountPrice());p.setStockQuantity(f.getStockQuantity());p.setIsActive(f.isActive());}
    private ProductForm toForm(Product p){ProductForm f=new ProductForm();f.setCategoryId(p.getCategory().getId());f.setName(p.getName());f.setSku(p.getSku());f.setDescription(p.getDescription());f.setSpecifications(p.getSpecifications());f.setPrice(p.getPrice());f.setDiscountPrice(p.getDiscountPrice());f.setStockQuantity(p.getStockQuantity());f.setActive(Boolean.TRUE.equals(p.getIsActive()));return f;}
    private String slug(String name){String base=Normalizer.normalize(name,Normalizer.Form.NFD).replaceAll("\\p{M}+","").replace('đ','d').replace('Đ','D').toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("^-+|-+$","");if(base.isBlank())base="san-pham";base=base.substring(0,Math.min(base.length(),240));return base+"-"+UUID.randomUUID().toString().substring(0,8);}
    private ResponseStatusException notFound(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy sản phẩm.");}
}
