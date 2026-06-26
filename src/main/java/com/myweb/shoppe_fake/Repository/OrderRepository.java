package com.myweb.shoppe_fake.Repository;

import com.myweb.shoppe_fake.Model.Order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByUserId(String userId); // Lấy lịch sử mua hàng của 1 user
    Page<Order> findByPhoneContainingIgnoreCaseOrStatusContainingIgnoreCase(String phone, String status, Pageable pageable);
    List<Order> findByStatus(String status);
    @Query(value = "{ 'details.productId' : ?0 }", exists = true)
    boolean existsByProductId(String productId);
}