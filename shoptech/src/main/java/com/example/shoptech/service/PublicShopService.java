package com.example.shoptech.service;

import com.example.shoptech.dto.PublicShop;
import com.example.shoptech.entity.Shop;
import com.example.shoptech.repository.ShopRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service @Transactional(readOnly=true)
public class PublicShopService {
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private final ShopRepository shops;
    public PublicShopService(ShopRepository shops) { this.shops=shops; }
    public List<PublicShop> options() { return shops.findPublicOptions().stream().map(PublicShopService::view).toList(); }
    public PublicShop bySlug(String slug) { return view(shops.findPublicBySlug(slug).orElseThrow(PublicShopService::missing)); }
    public PublicShop byId(Long id) { return view(shops.findPublicById(id).orElseThrow(PublicShopService::missing)); }
    public org.springframework.data.domain.Page<PublicShop> search(String keyword,int page,int size) {
        String text=keyword==null?"":keyword.strip();
        if(text.length()>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Từ khóa tối đa 100 ký tự.");
        var cb=entityManager.getCriteriaBuilder();
        var count=cb.createQuery(Long.class);var root=count.from(Shop.class);
        count.select(cb.count(root)).where(publicPredicates(cb,root,text));long total=entityManager.createQuery(count).getSingleResult();
        int normalizedSize=Math.max(1,Math.min(size,12));int current=(int)Math.min(Math.max(0,page),Math.max(0,(total-1)/normalizedSize));
        var query=cb.createQuery(Shop.class);var shop=query.from(Shop.class);
        query.select(shop).where(publicPredicates(cb,shop,text)).orderBy(cb.asc(cb.lower(shop.get("name"))),cb.asc(shop.get("id")));
        var content=entityManager.createQuery(query).setFirstResult(current*normalizedSize).setMaxResults(normalizedSize).getResultList().stream().map(PublicShopService::view).toList();
        return new org.springframework.data.domain.PageImpl<>(content,org.springframework.data.domain.PageRequest.of(current,normalizedSize),total);
    }
    private jakarta.persistence.criteria.Predicate[] publicPredicates(jakarta.persistence.criteria.CriteriaBuilder cb,jakarta.persistence.criteria.Root<Shop> root,String keyword) {
        String pattern="%"+keyword.replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
        return new jakarta.persistence.criteria.Predicate[]{cb.equal(root.get("status"),com.example.shoptech.entity.ShopStatus.ACTIVE),cb.isTrue(root.get("owner").get("isActive")),cb.isFalse(root.get("owner").get("isDeleted")),cb.like(cb.lower(root.get("name")),cb.lower(cb.literal(pattern)),'\\')};
    }
    private static PublicShop view(Shop s) { return new PublicShop(s.getId(),s.getName(),s.getSlug(),s.getDescription(),s.getCreatedAt()); }
    private static ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND,"Shop không tồn tại hoặc hiện chưa hoạt động."); }
}
