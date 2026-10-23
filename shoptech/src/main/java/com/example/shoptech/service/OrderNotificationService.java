package com.example.shoptech.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.*;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrderNotificationService {
    private static final Logger log=LoggerFactory.getLogger(OrderNotificationService.class);
    private final Map<String,Map<String,WebSocketSession>> sessions=new ConcurrentHashMap<>();
    private final ObjectMapper mapper;
    private final com.example.shoptech.repository.UserRepository users;
    public OrderNotificationService(ObjectMapper mapper, com.example.shoptech.repository.UserRepository users) { this.mapper=mapper; this.users=users; }
    public void register(WebSocketSession session) {
        sessions.computeIfAbsent(session.getPrincipal().getName(),key->new ConcurrentHashMap<>())
                .put(session.getId(),new ConcurrentWebSocketSessionDecorator(session,1000,65536));
    }
    public void remove(WebSocketSession session) {
        if (session.getPrincipal()==null) return;
        sessions.computeIfPresent(session.getPrincipal().getName(),(name,open)->{open.remove(session.getId());return open.isEmpty()?null:open;});
    }
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void afterCommit(OrderStatusChanged event) {
        Map<String,WebSocketSession> recipients=sessions.get(event.username()); if(recipients==null) return;
        final boolean active;
        try {
            // One fresh short read per recipient/event, completed before any socket I/O.
            active=users.existsByUsernameAndIsActiveTrueAndIsDeletedFalse(event.username());
        } catch(RuntimeException exception) {
            log.warn("Order notification authorization could not be verified; delivery skipped.");
            return;
        }
        for(WebSocketSession session:List.copyOf(recipients.values())) {
            try {
                Instant expiry=(Instant)session.getAttributes().get("jwtExpiry");
                if(!active || !session.isOpen() || expiry==null || !expiry.isAfter(Instant.now())) {
                    session.close(CloseStatus.POLICY_VIOLATION);remove(session);continue;
                }
                session.sendMessage(new TextMessage(mapper.writeValueAsString(event.notification())));
            } catch(Exception exception) {
                // Delivery is best effort after commit; the database remains the source of truth.
                remove(session); try {session.close(CloseStatus.SERVER_ERROR);} catch(Exception ignored) {}
                log.warn("Order notification delivery failed; client can reload its order.");
            }
        }
    }
}
