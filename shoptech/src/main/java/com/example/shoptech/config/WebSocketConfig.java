package com.example.shoptech.config;

import com.example.shoptech.security.JwtCookieAuthenticationFilter;
import com.example.shoptech.service.OrderNotificationService;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.http.server.*;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import java.util.Map;

@Configuration @EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final OrderNotificationService notifications; private final JwtDecoder decoder; private final String origin;
    public WebSocketConfig(OrderNotificationService notifications,JwtDecoder decoder,@Value("${shoptech.public-base-url:http://localhost:8080}") String baseUrl) {
        this.notifications=notifications;this.decoder=decoder;
        java.net.URI uri=java.net.URI.create(baseUrl); this.origin=uri.getScheme()+"://"+uri.getRawAuthority();
    }
    @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new OrderStatusHandler(notifications),"/ws/order-status")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override public boolean beforeHandshake(ServerHttpRequest request,ServerHttpResponse response,WebSocketHandler handler,Map<String,Object> attributes) {
                        if(!(request.getPrincipal() instanceof Authentication auth) || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken
                                || !origin.equals(request.getHeaders().getOrigin()) || !(request instanceof ServletServerHttpRequest servlet)) {
                            response.setStatusCode(HttpStatus.FORBIDDEN);return false;
                        }
                        Cookie[] cookies=servlet.getServletRequest().getCookies();
                        if(cookies!=null) for(Cookie cookie:cookies) if(JwtCookieAuthenticationFilter.COOKIE_NAME.equals(cookie.getName())) {
                            try {
                                Jwt jwt=decoder.decode(cookie.getValue());
                                if(!auth.getName().equals(jwt.getSubject()) || jwt.getExpiresAt()==null) break;
                                attributes.put("jwtExpiry",jwt.getExpiresAt()); return true;
                            }catch(JwtException ignored) {break;}
                        }
                        response.setStatusCode(HttpStatus.FORBIDDEN);return false;
                    }
                    @Override public void afterHandshake(ServerHttpRequest request,ServerHttpResponse response,WebSocketHandler handler,Exception exception) {}
                }).setAllowedOrigins(origin);
    }
    private static class OrderStatusHandler extends TextWebSocketHandler {
        private final OrderNotificationService notifications;
        OrderStatusHandler(OrderNotificationService notifications) {this.notifications=notifications;}
        @Override public void afterConnectionEstablished(WebSocketSession session) {notifications.register(session);}
        @Override public void afterConnectionClosed(WebSocketSession session,CloseStatus status) {notifications.remove(session);}
        @Override protected void handleTextMessage(WebSocketSession session,TextMessage message) throws Exception {
            // Server-to-client only: no client-supplied destination or user identifier is accepted.
            session.close(CloseStatus.POLICY_VIOLATION);
        }
    }
}
