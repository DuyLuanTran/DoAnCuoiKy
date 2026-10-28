package com.example.shoptech.service;

import com.example.shoptech.entity.*;
import com.example.shoptech.repository.*;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@Service @Transactional(readOnly=true)
public class ProductCommunityService {
    private final ProductFavoriteRepository favorites;private final ProductReviewRepository reviews;
    private final ProductRepository products;private final UserRepository users;private final EntityManager em;
    private final ReviewMediaService media;private final ShopService shops;
    public ProductCommunityService(ProductFavoriteRepository favorites,ProductReviewRepository reviews,ProductRepository products,UserRepository users,EntityManager em,ReviewMediaService media,ShopService shops){this.favorites=favorites;this.reviews=reviews;this.products=products;this.users=users;this.em=em;this.media=media;this.shops=shops;}
    public record Summary(double average,long count) {}
    public record Attachment(String url,String kind) {}
    public record ReviewView(Long id,long version,String author,int rating,String content,Instant createdAt,List<Attachment> media,String sellerReply,Instant repliedAt,boolean hidden,String reason,String productName,String productSlug) {}
    public record Eligibility(boolean allowed,String message) {}
    private User active(String name){return users.findByUsernameAndIsDeletedFalse(name).filter(u->Boolean.TRUE.equals(u.getIsActive())).orElseThrow(()->fail(HttpStatus.FORBIDDEN,"Tài khoản không hoạt động."));}
    private User locked(String name){return users.lockActiveByUsername(name).orElseThrow(()->fail(HttpStatus.FORBIDDEN,"Tài khoản không hoạt động."));}
    public Product visible(Long id){Product p=products.findById(id).orElseThrow(()->fail(HttpStatus.NOT_FOUND,"Không tìm thấy sản phẩm."));return products.findVisibleBySlug(p.getSlug()).orElseThrow(()->fail(HttpStatus.NOT_FOUND,"Sản phẩm không còn được bán."));}
    @PreAuthorize("isAuthenticated()")
    public Set<Long> selected(String username,Collection<Long> ids){active(username);if(ids.size()>100)throw fail(HttpStatus.BAD_REQUEST,"Tối đa 100 sản phẩm mỗi lần.");return ids.isEmpty()?Set.of():favorites.selected(username,ids);}
    @PreAuthorize("isAuthenticated()") @Transactional
    public boolean favorite(String username,Long id,boolean selected){
        User u=locked(username);var old=favorites.findByUserIdAndProductId(u.getId(),id);
        if(!selected){old.ifPresent(favorites::delete);return false;}
        Product p=visible(id);if(old.isEmpty()){ProductFavorite f=new ProductFavorite();f.setUser(u);f.setProduct(p);favorites.saveAndFlush(f);}return true;
    }
    @PreAuthorize("isAuthenticated()")
    public Page<Product> favorites(String username,int page){active(username);return favorites.visible(username,PageRequest.of(Math.max(0,page),12));}
    public Map<Long,Summary> summaries(Collection<Long> ids){
        if(ids.size()>100)throw fail(HttpStatus.BAD_REQUEST,"Tối đa 100 sản phẩm mỗi lần.");Map<Long,Summary> result=new HashMap<>();
        if(!ids.isEmpty())for(Object[] row:reviews.summaries(ids))result.put((Long)row[0],new Summary(((Number)row[1]).doubleValue(),((Number)row[2]).longValue()));return result;
    }
    public Page<ReviewView> publicReviews(Long id,int page){visible(id);return reviews.findByProductIdAndHiddenFalseOrderByCreatedAtDescIdDesc(id,PageRequest.of(Math.max(0,page),5)).map(this::view);}
    @PreAuthorize("isAuthenticated()")
    public Eligibility eligibility(String username,Long id){
        User u=active(username);Product p=visible(id);
        if(reviews.existsByUserIdAndProductId(u.getId(),id))return new Eligibility(false,"Bạn đã đánh giá sản phẩm này. Mỗi tài khoản đánh giá một lần.");
        if(p.getShop().getOwner().getId().equals(u.getId()))return new Eligibility(false,"Bạn không thể đánh giá sản phẩm của shop mình.");
        return purchase(u,p).isPresent()?new Eligibility(true,"Bạn đã mua sản phẩm. Hãy chia sẻ trải nghiệm của mình."):new Eligibility(false,"Bạn có thể đánh giá sau khi mua và đơn của shop đã COMPLETED.");
    }
    private Optional<OrderDetail> purchase(User u,Product p){
        var cb=em.getCriteriaBuilder();var q=cb.createQuery(OrderDetail.class);var d=q.from(OrderDetail.class);
        q.select(d).where(cb.equal(d.get("order").get("user").get("id"),u.getId()),cb.equal(d.get("productId"),p.getId()),cb.equal(d.get("shopOrder").get("shop").get("id"),p.getShop().getId()),ProductSalesCriteria.eligible(q,cb,d)).orderBy(cb.desc(d.get("id")));
        return em.createQuery(q).setMaxResults(1).getResultStream().findFirst();
    }
    @PreAuthorize("isAuthenticated()") @Transactional
    public ReviewView create(String username,Long id,int rating,String content,List<MultipartFile> files){
        User u=locked(username);Product p=visible(id);String text=normalize(content,50,2000,"Bình luận cần từ 50 đến 2000 ký tự.");
        if(rating<1||rating>5)throw fail(HttpStatus.BAD_REQUEST,"Chọn từ 1 đến 5 sao.");
        if(p.getShop().getOwner().getId().equals(u.getId()))throw fail(HttpStatus.FORBIDDEN,"Không thể đánh giá shop của mình.");
        if(reviews.existsByUserIdAndProductId(u.getId(),id))throw fail(HttpStatus.CONFLICT,"Bạn đã đánh giá sản phẩm này.");
        OrderDetail line=purchase(u,p).orElseThrow(()->fail(HttpStatus.FORBIDDEN,"Chỉ được đánh giá sản phẩm đã mua từ đơn COMPLETED."));
        // Share the order-row lock used by fulfilment/refunds; re-check eligibility after waiting.
        em.lock(line.getOrder(),jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        em.refresh(line.getShopOrder());if(purchase(u,p).isEmpty())throw fail(HttpStatus.CONFLICT,"Trạng thái đơn đã thay đổi. Vui lòng tải lại.");
        List<ReviewMediaService.Checked> checked=media.validate(files);List<ReviewAttachment> uploaded=new ArrayList<>();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)uploaded.forEach(media::discard);}});
        ProductReview r=new ProductReview();r.setUser(u);r.setProduct(p);r.setPurchase(line);r.setRating(rating);r.setContent(text);reviews.saveAndFlush(r);
        for(var file:checked){ReviewAttachment attachment=media.upload(file);uploaded.add(attachment);r.getMedia().add(attachment);}
        reviews.flush();return view(r);
    }
    @PreAuthorize("(#seller and hasRole('SELLER')) or (!#seller and hasRole('ADMIN'))")
    public Page<ReviewView> manage(String username,boolean seller,int page){Long shopId=seller?shops.requiredOwn(username).getId():null;return reviews.manage(shopId,PageRequest.of(Math.max(0,page),10)).map(this::view);}
    @PreAuthorize("hasRole('SELLER')") @Transactional
    public void reply(String username,Long id,long version,String reply){
        User actor=locked(username);Long shopId=shops.requiredOwn(actor.getUsername()).getId();ProductReview r=reviews.lock(id).orElseThrow(()->fail(HttpStatus.NOT_FOUND,"Không tìm thấy đánh giá."));
        if(!r.getProduct().getShop().getId().equals(shopId))throw fail(HttpStatus.NOT_FOUND,"Không tìm thấy đánh giá.");
        checkVersion(r,version);if(r.isHidden())throw fail(HttpStatus.CONFLICT,"Đánh giá đang bị ẩn.");r.setSellerReply(normalize(reply,1,2000,"Phản hồi cần từ 1 đến 2000 ký tự."));r.setRepliedAt(Instant.now());reviews.flush();
    }
    @PreAuthorize("hasRole('ADMIN')") @Transactional
    public void moderate(String username,Long id,long version,boolean hidden,String reason){
        locked(username);ProductReview r=reviews.lock(id).orElseThrow(()->fail(HttpStatus.NOT_FOUND,"Không tìm thấy đánh giá."));checkVersion(r,version);
        String text=normalize(reason,5,1000,"Lý do cần từ 5 đến 1000 ký tự.");if(r.isHidden()==hidden)throw fail(HttpStatus.CONFLICT,"Trạng thái đã thay đổi.");
        r.setHidden(hidden);r.setModerationReason(text);ReviewModerationHistory h=new ReviewModerationHistory();h.setReview(r);h.setActor(username);h.setHidden(hidden);h.setReason(text);em.persist(h);reviews.flush();
    }
    private void checkVersion(ProductReview r,long version){if(r.getVersion()!=version)throw fail(HttpStatus.CONFLICT,"Đánh giá đã thay đổi. Hãy tải lại trang.");}
    private ReviewView view(ProductReview r){return new ReviewView(r.getId(),r.getVersion(),r.getUser().getUsername(),r.getRating(),r.getContent(),r.getCreatedAt(),r.getMedia().stream().map(a->new Attachment(a.getUrl(),a.getKind())).toList(),r.getSellerReply(),r.getRepliedAt(),r.isHidden(),r.getModerationReason(),r.getProduct().getName(),r.getProduct().getSlug());}
    private static String normalize(String s,int min,int max,String message){String text=Objects.toString(s,"").strip();if(text.codePointCount(0,text.length())<min||text.codePointCount(0,text.length())>max)throw fail(HttpStatus.BAD_REQUEST,message);return text;}
    private static ResponseStatusException fail(HttpStatus status,String message){return new ResponseStatusException(status,message);}
}
