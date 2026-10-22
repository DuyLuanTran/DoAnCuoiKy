package com.example.shoptech.service;
import com.example.shoptech.dto.OrderStatusNotification;
import com.example.shoptech.entity.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.*;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class OrderNotificationServiceTest {
    final com.example.shoptech.repository.UserRepository users=mock(com.example.shoptech.repository.UserRepository.class);
    @Test void expiredJwtDoesNotReceiveData() throws Exception {
        WebSocketSession session=mock(WebSocketSession.class);when(session.getPrincipal()).thenReturn(()->"alice");when(session.getId()).thenReturn("one");
        when(session.isOpen()).thenReturn(true);when(session.getAttributes()).thenReturn(Map.of("jwtExpiry",Instant.now().minusSeconds(1)));
        when(users.existsByUsernameAndIsActiveTrueAndIsDeletedFalse("alice")).thenReturn(true);
        OrderNotificationService service=new OrderNotificationService(new ObjectMapper(),users);service.register(session);
        service.afterCommit(new OrderStatusChanged("alice",new OrderStatusNotification(1L,"ORD-test",OrderStatus.CONFIRMED)));
        verify(session,never()).sendMessage(any());verify(session).close(CloseStatus.POLICY_VIOLATION);
    }
    @Test void brokenTransportDoesNotThrowBackIntoOrder() throws Exception {
        WebSocketSession session=mock(WebSocketSession.class);when(session.getPrincipal()).thenReturn(()->"alice");when(session.getId()).thenReturn("two");
        when(session.isOpen()).thenReturn(true);when(session.getAttributes()).thenReturn(Map.of("jwtExpiry",Instant.now().plusSeconds(60)));
        doThrow(new java.io.IOException("test disconnect")).when(session).sendMessage(any());
        when(users.existsByUsernameAndIsActiveTrueAndIsDeletedFalse("alice")).thenReturn(true);
        OrderNotificationService service=new OrderNotificationService(new ObjectMapper(),users);service.register(session);
        assertDoesNotThrow(()->service.afterCommit(new OrderStatusChanged("alice",new OrderStatusNotification(1L,"ORD-test",OrderStatus.CONFIRMED))));
        verify(session).close(CloseStatus.SERVER_ERROR);
    }
    @Test void authorizationReadFailureSkipsDeliveryWithoutBreakingOrder() throws Exception {
        WebSocketSession session=mock(WebSocketSession.class);when(session.getPrincipal()).thenReturn(()->"alice");when(session.getId()).thenReturn("three");
        when(users.existsByUsernameAndIsActiveTrueAndIsDeletedFalse("alice")).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("isolated test"));
        OrderNotificationService service=new OrderNotificationService(new ObjectMapper(),users);service.register(session);
        assertDoesNotThrow(()->service.afterCommit(new OrderStatusChanged("alice",new OrderStatusNotification(1L,"ORD-test",OrderStatus.CONFIRMED))));
        verify(session,never()).sendMessage(any());
    }
}
