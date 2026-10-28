package com.example.shoptech.repository;

import com.example.shoptech.entity.OrderNotification;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderNotificationRepository extends JpaRepository<OrderNotification, Long> {
    Page<OrderNotification> findByRecipientUsernameOrderByCreatedAtDescIdDesc(String username, Pageable pageable);
    long countByRecipientUsernameAndReadAtIsNull(String username);
    Optional<OrderNotification> findByIdAndRecipientUsername(Long id, String username);
}
