package com.example.shoptech.repository;
import com.example.shoptech.entity.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface ShopRepository extends JpaRepository<Shop,Long> {
    @Query("select s from Shop s where s.slug=:slug and s.status=com.example.shoptech.entity.ShopStatus.ACTIVE and s.owner.isActive=true and s.owner.isDeleted=false")
    Optional<Shop> findPublicBySlug(@Param("slug") String slug);

    @Query("select s from Shop s where s.id=:id and s.status=com.example.shoptech.entity.ShopStatus.ACTIVE and s.owner.isActive=true and s.owner.isDeleted=false")
    Optional<Shop> findPublicById(@Param("id") Long id);

    @Query("select s from Shop s where s.status=com.example.shoptech.entity.ShopStatus.ACTIVE and s.owner.isActive=true and s.owner.isDeleted=false order by lower(s.name),s.id")
    java.util.List<Shop> findPublicOptions();
    @EntityGraph(attributePaths={"owner","reviewedBy"}) Optional<Shop> findByOwnerUsername(String username);
    @EntityGraph(attributePaths={"owner","reviewedBy"}) Optional<Shop> findById(Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from Shop s where s.id=:id") Optional<Shop> lockById(@Param("id") Long id);
    long countByStatus(ShopStatus status);
    @EntityGraph(attributePaths="owner")
    @Query("select s from Shop s where (:status is null or s.status=:status) and (lower(s.name) like lower(concat('%',:keyword,'%')) or lower(s.owner.username) like lower(concat('%',:keyword,'%')))")
    Page<Shop> search(@Param("keyword") String keyword,@Param("status") ShopStatus status,Pageable pageable);
}
