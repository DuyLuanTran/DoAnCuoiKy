package com.example.shoptech;

import com.example.shoptech.dto.*;
import com.example.shoptech.entity.*;
import com.example.shoptech.repository.*;
import com.example.shoptech.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
        "spring.config.location=classpath:/application-test.properties","spring.datasource.url=jdbc:h2:mem:ws-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"})
class WebSocketNotificationTest {
    @LocalServerPort int port;
    @Autowired UserRepository users; @Autowired CategoryRepository categories; @Autowired ProductRepository products; @Autowired ShopRepository shops;
    @Autowired CartService carts; @Autowired OrderService orders; @Autowired SellerOrderService seller;@Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired JwtService jwt; @Autowired PlatformTransactionManager transactions;
    @Autowired org.springframework.security.oauth2.jwt.JwtEncoder jwtEncoder;
    String alice,bob,sellerName; Long productId;
    @BeforeEach void seed() {
        String suffix=UUID.randomUUID().toString().substring(0,8);alice="alice-"+suffix;bob="bob-"+suffix;sellerName="seller-"+suffix;
        new TransactionTemplate(transactions).executeWithoutResult(s->{
            for(String name:List.of(alice,bob,sellerName))users.save(User.builder().username(name).email(name+"@example.invalid").password("unused").fullName("Test").build());
            Category category=new Category();category.setName("Test");category.setSlug(suffix);categories.save(category);
            Product product=new Product();product.setShop(TestShops.active(shops,users.findByUsernameAndIsDeletedFalse(sellerName).orElseThrow(),suffix));product.setName("Test");product.setCategory(category);product.setSlug(suffix);product.setPrice(BigDecimal.TEN);product.setStockQuantity(5);productId=products.saveAndFlush(product).getId();
        });
    }
    static class Inbox implements WebSocket.Listener {
        final BlockingQueue<String> messages=new LinkedBlockingQueue<>(); final CompletableFuture<Integer> closed=new CompletableFuture<>(); final StringBuilder buffer=new StringBuilder();
        public void onOpen(WebSocket socket){socket.request(1);}
        public CompletionStage<?> onText(WebSocket socket,CharSequence text,boolean last){buffer.append(text);if(last){messages.add(buffer.toString());buffer.setLength(0);}socket.request(1);return CompletableFuture.completedFuture(null);}
        public CompletionStage<?> onClose(WebSocket socket,int status,String reason){closed.complete(status);return CompletableFuture.completedFuture(null);}
    }
    WebSocket connect(String username,String origin,Inbox inbox) {
        var builder=HttpClient.newHttpClient().newWebSocketBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).header("Origin",origin);
        if(username!=null)builder.header("Cookie","SHOPTECH_AUTH="+jwt.createToken(username));
        return builder.buildAsync(URI.create("ws://localhost:"+port+"/ws/order-status"),inbox).join();
    }
    OrderDto sellerTransition(Long orderId,OrderDto.Transition input){
        Long child=jdbc.queryForObject("select id from shop_orders where order_id=?",Long.class,orderId);
        var before=org.springframework.security.core.context.SecurityContextHolder.getContext();var context=org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();context.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(sellerName,"unused",List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SELLER"))));org.springframework.security.core.context.SecurityContextHolder.setContext(context);
        try{return seller.transition(sellerName,child,input,"Notification test");}finally{org.springframework.security.core.context.SecurityContextHolder.setContext(before);}
    }
    OrderDto place() {carts.add(alice,productId,1);return orders.checkout(alice,new OrderDto.Checkout("Test","0901234567","Test address",null,"COD",UUID.randomUUID().toString()));}
    @Test @WithMockUser(roles="ADMIN") void statusChangeOnlyReachesOwnerAfterCommit() throws Exception {
        Inbox owner=new Inbox(),other=new Inbox();WebSocket a=connect(alice,"http://localhost:8080",owner),b=connect(bob,"http://localhost:8080",other);
        try {
            OrderDto order=place();sellerTransition(order.id(),new OrderDto.Transition(OrderStatus.PENDING,OrderStatus.CONFIRMED));
            String message=owner.messages.poll(5,TimeUnit.SECONDS);assertNotNull(message);assertTrue(message.contains("CONFIRMED"));assertTrue(message.contains(order.code()));
            assertFalse(message.contains("0901234567"));assertFalse(message.contains("Test address"));assertNull(other.messages.poll(300,TimeUnit.MILLISECONDS));
            assertEquals(OrderStatus.CONFIRMED,orders.owned(alice,order.id()).status());
        }finally{a.abort();b.abort();}
    }
    @Test @WithMockUser(roles="ADMIN") void rollbackDoesNotNotifyAndDisconnectDoesNotBreakOrder() throws Exception {
        Inbox inbox=new Inbox();WebSocket socket=connect(alice,"http://localhost:8080",inbox);OrderDto order=place();
        try {
            new TransactionTemplate(transactions).executeWithoutResult(s->{sellerTransition(order.id(),new OrderDto.Transition(OrderStatus.PENDING,OrderStatus.CONFIRMED));s.setRollbackOnly();});
            assertNull(inbox.messages.poll(300,TimeUnit.MILLISECONDS));assertEquals(OrderStatus.PENDING,orders.owned(alice,order.id()).status());
        }finally{socket.abort();}
        assertEquals(OrderStatus.CONFIRMED,sellerTransition(order.id(),new OrderDto.Transition(OrderStatus.PENDING,OrderStatus.CONFIRMED)).status());
    }
    @Test void rejectsAnonymousAndForeignOrigin() {
        assertThrows(CompletionException.class,()->connect(null,"http://localhost:8080",new Inbox()));
        assertThrows(CompletionException.class,()->connect(alice,"https://attacker.invalid",new Inbox()));
    }
    @Test void rejectsExpiredSignedJwtAtRealHandshake() {
        var now=java.time.Instant.now();
        var claims=org.springframework.security.oauth2.jwt.JwtClaimsSet.builder().issuer("shoptech").subject(alice)
                .issuedAt(now.minusSeconds(7200)).expiresAt(now.minusSeconds(3600)).build();
        String expired=jwtEncoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(),claims)).getTokenValue();
        var failure=assertThrows(CompletionException.class,()->HttpClient.newHttpClient().newWebSocketBuilder()
                .header("Origin","http://localhost:8080").header("Cookie","SHOPTECH_AUTH="+expired)
                .buildAsync(URI.create("ws://localhost:"+port+"/ws/order-status"),new Inbox()).join());
        assertInstanceOf(java.net.http.WebSocketHandshakeException.class,failure.getCause());
        int status=((java.net.http.WebSocketHandshakeException)failure.getCause()).getResponse().statusCode();
        assertTrue(status==302 || status==401 || status==403);
    }
    @Test void clientsCannotChooseOtherUsersDestination() throws Exception {
        Inbox inbox=new Inbox();WebSocket socket=connect(alice,"http://localhost:8080",inbox);
        try{socket.sendText("SUBSCRIBE /user/"+bob+"/queue/order-status",true).join();assertEquals(1008,inbox.closed.get(5,TimeUnit.SECONDS));}finally{socket.abort();}
    }
    @Test @WithMockUser(roles="ADMIN") void reconnectReceivesLaterNotification() throws Exception {
        Inbox first=new Inbox();WebSocket socket=connect(alice,"http://localhost:8080",first);socket.abort();
        Inbox second=new Inbox();WebSocket fresh=connect(alice,"http://localhost:8080",second);
        try{OrderDto order=place();sellerTransition(order.id(),new OrderDto.Transition(OrderStatus.PENDING,OrderStatus.CONFIRMED));assertNotNull(second.messages.poll(5,TimeUnit.SECONDS));}finally{fresh.abort();}
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    @WithMockUser(roles="ADMIN")
    void lockedOrDeletedAccountCannotReceiveOnExistingSocket(boolean deleted) throws Exception {
        Inbox inbox=new Inbox();WebSocket socket=connect(alice,"http://localhost:8080",inbox);OrderDto order=place();
        try {
            new TransactionTemplate(transactions).executeWithoutResult(s->{
                User user=users.findByUsernameAndIsDeletedFalse(alice).orElseThrow();
                if(deleted)user.setIsDeleted(true);else user.setIsActive(false);
            });
            sellerTransition(order.id(),new OrderDto.Transition(OrderStatus.PENDING,OrderStatus.CONFIRMED));
            assertNull(inbox.messages.poll(500,TimeUnit.MILLISECONDS));
            assertEquals(1008,inbox.closed.get(5,TimeUnit.SECONDS));
        }finally{socket.abort();}
    }
}
