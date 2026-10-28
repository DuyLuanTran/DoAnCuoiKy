package com.example.shoptech;

import com.example.shoptech.entity.*;
import com.example.shoptech.repository.*;
import com.example.shoptech.service.CartService;
import com.example.shoptech.service.OrderService;
import com.example.shoptech.dto.OrderDto;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.math.BigDecimal;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties = "spring.config.location=classpath:/application-test.properties")
@AutoConfigureMockMvc
class CommerceIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired ShopRepository shops;
    @Autowired CategoryRepository categories;
    @Autowired CartRepository carts;
    @Autowired CartItemRepository items;
    @Autowired CartService cartService;
    @Autowired OrderService orderService;
    @Autowired OrderRepository orders;
    @Autowired com.example.shoptech.service.AdminOrderService adminOrders;
    @Autowired com.example.shoptech.service.SellerOrderService sellerOrders;
    @Autowired com.example.shoptech.service.UserService userService;
    @Autowired com.example.shoptech.service.RevenueService revenue;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @Autowired PlatformTransactionManager manager;
    Long productId;
    @BeforeEach void seed() {
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            entityManager.createQuery("delete from ProductModerationHistory").executeUpdate();
            entityManager.createQuery("delete from ShopOrderHistory").executeUpdate();entityManager.createQuery("delete from OrderDetail").executeUpdate();entityManager.createQuery("delete from ShopOrder").executeUpdate(); orders.deleteAllInBatch();
            items.deleteAllInBatch(); carts.deleteAllInBatch(); products.deleteAllInBatch(); categories.deleteAllInBatch(); entityManager.createQuery("delete from ShopReviewHistory").executeUpdate(); shops.deleteAllInBatch(); users.deleteAllInBatch();
            users.save(User.builder().username("alice").email("alice@example.invalid").password("unused").fullName("Alice Test").build());
            users.save(User.builder().username("bob").email("bob@example.invalid").password("unused").fullName("Bob Test").build());
            Category category = new Category(); category.setName("Test"); category.setSlug("test"); categories.save(category);
            Product product = new Product(); product.setShop(TestShops.active(shops,users.findByUsernameAndIsDeletedFalse("alice").orElseThrow(),"commerce-shop")); product.setCategory(category); product.setName("Test product"); product.setSlug("test-product"); product.setPrice(new BigDecimal("100")); product.setStockQuantity(10);
            productId = products.saveAndFlush(product).getId();
        });
    }
    @Test void cartRequiresLogin() throws Exception { mvc.perform(get("/api/cart")).andExpect(status().isUnauthorized()); }
    @Test void resetPasswordFormRendersWithAndWithoutToken() throws Exception {
        mvc.perform(get("/reset-password"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tạo mật khẩu mới")));
        mvc.perform(get("/reset-password").param("token","test-token"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"token\"")));
    }
    @Test @WithMockUser(username="alice") void csrfRequired() throws Exception { mvc.perform(post("/api/cart/items").contentType("application/json").content("{\"productId\":"+productId+",\"quantity\":1}")).andExpect(status().isForbidden()); }
    @Test @WithMockUser(username="alice") void cartApiAndPage() throws Exception {
        mvc.perform(post("/api/cart/items").with(csrf()).contentType("application/json").content("{\"productId\":"+productId+",\"quantity\":2}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(200));
        mvc.perform(get("/cart")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("cart-content")));
        mvc.perform(get("/products")).andExpect(status().isOk());
        mvc.perform(get("/products/test-product")).andExpect(status().isOk());
    }
    @Test void concurrentFirstAddCreatesOneCartAndMerges() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<Void> add = () -> { start.await(); cartService.add("alice",productId,1); return null; };
            Future<Void> a = pool.submit(add), b = pool.submit(add); start.countDown(); a.get(20,TimeUnit.SECONDS); b.get(20,TimeUnit.SECONDS);
            assertEquals(1,carts.count()); assertEquals(1,items.count()); assertEquals(2,cartService.get("alice").quantity());
            assertEquals(10,products.findById(productId).orElseThrow().getStockQuantity());
        } finally { pool.shutdownNow(); }
    }
    @Test @WithMockUser(username="bob") void otherUsersItemIsNotAccessible() throws Exception {
        Long id = cartService.add("alice",productId,1).items().get(0).id();
        mvc.perform(delete("/api/cart/items/"+id).with(csrf())).andExpect(status().isNotFound());
        assertEquals(1,cartService.get("alice").quantity());
    }
    OrderDto.Checkout checkoutInput() { return new OrderDto.Checkout("Test Buyer","0901234567","Test address",null,"COD",java.util.UUID.randomUUID().toString()); }
    @Test @WithMockUser(username="alice") void checkoutSnapshotAndHistory() throws Exception {
        cartService.add("alice",productId,2);
        new TransactionTemplate(manager).executeWithoutResult(s -> { Product p=products.findById(productId).orElseThrow(); p.setDiscountPrice(new BigDecimal("80")); });
        OrderDto order=orderService.checkout("alice",checkoutInput());
        assertEquals(new BigDecimal("160.00"),order.total()); assertEquals("Test product",order.items().get(0).name());
        assertEquals(8,products.findById(productId).orElseThrow().getStockQuantity()); assertEquals(0,items.count());
        new TransactionTemplate(manager).executeWithoutResult(s -> products.findById(productId).orElseThrow().setPrice(new BigDecimal("999")));
        assertEquals(new BigDecimal("80.00"),orderService.owned("alice",order.id()).items().get(0).price());
        mvc.perform(get("/orders")).andExpect(status().isOk()); mvc.perform(get("/orders/"+order.id())).andExpect(status().isOk()); mvc.perform(get("/checkout")).andExpect(status().isOk());
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> orderService.owned("bob",order.id()));
    }
    @Test void rejectsEmptyCheckout() { assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> orderService.checkout("alice",checkoutInput())); assertEquals(0,orders.count()); }
    @Test void checkoutRechecksStockAndAvailability() {
        cartService.add("alice",productId,2);
        new TransactionTemplate(manager).executeWithoutResult(s -> products.findById(productId).orElseThrow().setStockQuantity(1));
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> orderService.checkout("alice",checkoutInput()));
        assertEquals(0,orders.count()); assertEquals(1,items.count()); assertEquals(1,products.findById(productId).orElseThrow().getStockQuantity());
        new TransactionTemplate(manager).executeWithoutResult(s -> products.findById(productId).orElseThrow().setIsActive(false));
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> orderService.checkout("alice",checkoutInput()));
    }
    @Test void rollbackRestoresEarlierProductWhenLaterLineFails() {
        cartService.add("alice",productId,1);
        Long second = new TransactionTemplate(manager).execute(s -> { Product p=new Product();p.setShop(products.findById(productId).orElseThrow().getShop()); p.setCategory(categories.findAll().get(0)); p.setName("Second"); p.setSlug("second"); p.setPrice(BigDecimal.TEN); p.setStockQuantity(1); return products.saveAndFlush(p).getId(); });
        cartService.add("alice",second,1);
        new TransactionTemplate(manager).executeWithoutResult(s -> products.findById(second).orElseThrow().setIsDeleted(true));
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> orderService.checkout("alice",checkoutInput()));
        assertEquals(10,products.findById(productId).orElseThrow().getStockQuantity()); assertEquals(0,orders.count()); assertEquals(2,items.count());
    }
    @Test void concurrentDoubleSubmitReturnsOneOrder() throws Exception {
        cartService.add("alice",productId,1); OrderDto.Checkout input=checkoutInput();
        ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch start=new CountDownLatch(1);
        try {
            Callable<OrderDto> task=()->{start.await();return orderService.checkout("alice",input);};
            Future<OrderDto> a=pool.submit(task),b=pool.submit(task); start.countDown();
            assertEquals(a.get(20,TimeUnit.SECONDS).id(),b.get(20,TimeUnit.SECONDS).id()); assertEquals(1,orders.count());
            assertEquals(9,products.findById(productId).orElseThrow().getStockQuantity());
        } finally {pool.shutdownNow();}
    }
    @Test void twoBuyersCompeteForLastStock() throws Exception {
        cartService.add("alice",productId,1); cartService.add("bob",productId,1);
        new TransactionTemplate(manager).executeWithoutResult(s -> products.findById(productId).orElseThrow().setStockQuantity(1));
        ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch start=new CountDownLatch(1);
        try {
            Callable<Boolean> alice=()->{start.await();try{orderService.checkout("alice",checkoutInput());return true;}catch(org.springframework.web.server.ResponseStatusException e){return false;}};
            Callable<Boolean> bob=()->{start.await();try{orderService.checkout("bob",checkoutInput());return true;}catch(org.springframework.web.server.ResponseStatusException e){return false;}};
            Future<Boolean> a=pool.submit(alice),b=pool.submit(bob);start.countDown(); assertNotEquals(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS));
            assertEquals(1,orders.count());assertEquals(0,products.findById(productId).orElseThrow().getStockQuantity());assertEquals(1,items.count());
        } finally {pool.shutdownNow();}
    }
    @Test @WithMockUser(username="alice") void checkoutValidatesRecipientAndPayment() throws Exception {
        mvc.perform(post("/api/orders").with(csrf()).contentType("application/json").content("{}")) .andExpect(status().isBadRequest());
        assertEquals(0,orders.count());
    }
    OrderDto sellerTransition(Long orderId,OrderDto.Transition input){
        Long child=jdbc.queryForObject("select id from shop_orders where order_id=?",Long.class,orderId);
        var before=org.springframework.security.core.context.SecurityContextHolder.getContext();
        var context=org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("alice","unused",java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SELLER"))));
        org.springframework.security.core.context.SecurityContextHolder.setContext(context);
        try{return sellerOrders.transition("alice",child,input,"Integration test");}finally{org.springframework.security.core.context.SecurityContextHolder.setContext(before);}
    }
    OrderDto placeOrder() { cartService.add("alice",productId,1); return orderService.checkout("alice",checkoutInput()); }
    @Test @WithMockUser(username="alice",roles="USER") void adminEndpointsDenyUser() throws Exception {
        mvc.perform(get("/admin/orders")).andExpect(status().isForbidden());
        mvc.perform(patch("/admin/orders/1/status").with(csrf()).contentType("application/json").content("{\"expectedStatus\":\"PENDING\",\"status\":\"CONFIRMED\"}")).andExpect(status().isForbidden());
    }
    @Test @WithMockUser(roles="ADMIN") void adminSearchPaginationDetailAndTransitions() throws Exception {
        OrderDto order=placeOrder();
        mvc.perform(get("/admin/orders").param("keyword","alice")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString(order.code())));
        mvc.perform(get("/admin/orders").param("keyword","missing")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Không có đơn")));
        mvc.perform(get("/admin/orders/"+order.id())).andExpect(status().isOk());
        org.springframework.security.core.context.SecurityContextHolder.setContext(org.springframework.security.test.context.TestSecurityContextHolder.getContext());
        assertEquals(1,adminOrders.search("",null,0).getTotalElements()); assertEquals(0,adminOrders.search("",null,1).getContent().size());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->sellerTransition(order.id(),new OrderDto.Transition(OrderStatus.PENDING,OrderStatus.COMPLETED)));
        sellerTransition(order.id(),new OrderDto.Transition(OrderStatus.PENDING,OrderStatus.CONFIRMED));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->sellerTransition(order.id(),new OrderDto.Transition(OrderStatus.PENDING,OrderStatus.CANCELLED)));
        sellerTransition(order.id(),new OrderDto.Transition(OrderStatus.CONFIRMED,OrderStatus.PICKED_UP));
        sellerTransition(order.id(),new OrderDto.Transition(OrderStatus.PICKED_UP,OrderStatus.SHIPPING));
        OrderDto completed=sellerTransition(order.id(),new OrderDto.Transition(OrderStatus.SHIPPING,OrderStatus.COMPLETED));
        assertEquals("PAID",completed.paymentStatus());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->sellerTransition(order.id(),new OrderDto.Transition(OrderStatus.COMPLETED,OrderStatus.PENDING)));
    }
    @Test @WithMockUser(roles="ADMIN") void cancellationRestocksOnceAndIsTerminal() {
        OrderDto order=placeOrder();
        sellerTransition(order.id(),new OrderDto.Transition(OrderStatus.PENDING,OrderStatus.CANCELLED));
        assertEquals(10,products.findById(productId).orElseThrow().getStockQuantity());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->sellerTransition(order.id(),new OrderDto.Transition(OrderStatus.CANCELLED,OrderStatus.CONFIRMED)));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->sellerTransition(order.id(),new OrderDto.Transition(OrderStatus.PENDING,OrderStatus.CANCELLED)));
        assertEquals(10,products.findById(productId).orElseThrow().getStockQuantity());
    }
    @Test @WithMockUser(roles="ADMIN") void concurrentAdminsGetConflictAndRestockOnce() throws Exception {
        OrderDto order=placeOrder(); ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch start=new CountDownLatch(1);
        var context=org.springframework.security.core.context.SecurityContextHolder.getContext();
        try {
            Callable<Boolean> change=()->{org.springframework.security.core.context.SecurityContextHolder.setContext(context); try {start.await();sellerTransition(order.id(),new OrderDto.Transition(OrderStatus.PENDING,OrderStatus.CANCELLED));return true;}catch(org.springframework.web.server.ResponseStatusException e){assertEquals(409,e.getStatusCode().value());return false;}finally{org.springframework.security.core.context.SecurityContextHolder.clearContext();}};
            Future<Boolean> a=pool.submit(change),b=pool.submit(change);start.countDown();assertNotEquals(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS));
            assertEquals(10,products.findById(productId).orElseThrow().getStockQuantity());
        }finally{pool.shutdownNow();}
    }
    @Test @WithMockUser(roles="ADMIN") void revenueAggregatesWithTimezoneBoundariesAndNoDetailDoubleCounting() throws Exception {
        OrderDto jan=placeOrder(), feb=placeOrder(), cancelled=placeOrder(), pending=placeOrder(), deleted=placeOrder();
        jdbc.update("update orders set order_status='COMPLETED', created_at=? where id=?",java.time.OffsetDateTime.parse("2026-01-31T16:59:59Z"),jan.id());
        jdbc.update("update orders set order_status='COMPLETED', created_at=? where id=?",java.time.OffsetDateTime.parse("2026-01-31T17:00:00Z"),feb.id());
        jdbc.update("update orders set order_status='CANCELLED', created_at=? where id=?",java.time.OffsetDateTime.parse("2026-02-01T00:00:00Z"),cancelled.id());
        jdbc.update("update orders set created_at=? where id=?",java.time.OffsetDateTime.parse("2026-02-01T00:00:00Z"),pending.id());
        jdbc.update("update orders set order_status='COMPLETED', is_deleted=true, created_at=? where id=?",java.time.OffsetDateTime.parse("2026-02-01T00:00:00Z"),deleted.id());
        jdbc.update("update shop_orders s set status=(select order_status from orders o where o.id=s.order_id),created_at=(select created_at from orders o where o.id=s.order_id)");
        var report=revenue.report(java.time.LocalDate.parse("2026-01-31"),java.time.LocalDate.parse("2026-02-01"),com.example.shoptech.service.RevenueService.Group.DAY);
        assertEquals(new BigDecimal("200.00"),report.totalRevenue());assertEquals(2,report.completedOrders()); assertEquals(1,report.points().get(0).orders());assertEquals(1,report.points().get(1).orders());
        var monthly=revenue.report(java.time.LocalDate.parse("2026-01-01"),java.time.LocalDate.parse("2026-02-28"),com.example.shoptech.service.RevenueService.Group.MONTH);
        assertEquals("2026-01",monthly.points().get(0).label());assertEquals("2026-02",monthly.points().get(1).label());assertEquals(2,monthly.completedOrders());
        mvc.perform(get("/admin/revenue")).andExpect(status().isOk());
        mvc.perform(get("/admin/revenue/data").param("from","2026-01-31").param("to","2026-02-01")).andExpect(status().isOk()).andExpect(jsonPath("$.totalRevenue").value(200));
    }
    @Test @WithMockUser(roles="ADMIN") void revenueEmptyAndValidation() throws Exception {
        var report=revenue.report(java.time.LocalDate.parse("2026-01-01"),java.time.LocalDate.parse("2026-01-02"),com.example.shoptech.service.RevenueService.Group.DAY);
        assertEquals(0,report.totalRevenue().signum());assertEquals(0,report.completedOrders());assertEquals(2,report.points().size());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->revenue.report(java.time.LocalDate.parse("2026-02-01"),java.time.LocalDate.parse("2026-01-01"),com.example.shoptech.service.RevenueService.Group.DAY));
        mvc.perform(get("/admin/revenue/data").param("from","bad").param("to","2026-01-01")).andExpect(status().isBadRequest());
    }
    @Test @WithMockUser(roles="ADMIN") void revenueProjectionHandlesPostgresLowercaseAlias() {
        // Simulate identifier folding through a real Hibernate Tuple; this is not a PostgreSQL run.
        OrderDto order=placeOrder();
        jdbc.update("update orders set order_status='COMPLETED' where id=?",order.id());
        new TransactionTemplate(manager).executeWithoutResult(s -> {
            jakarta.persistence.Tuple tuple=(jakarta.persistence.Tuple)entityManager.createNativeQuery(
                    "select count(*) as \"ordercount\" from orders where order_status='COMPLETED'",jakarta.persistence.Tuple.class).getSingleResult();
            assertEquals("ordercount",tuple.getElements().get(0).getAlias());
            var projection=new org.springframework.data.projection.SpelAwareProxyProjectionFactory().createProjection(
                    OrderRepository.RevenueDay.class,new org.springframework.data.jpa.util.TupleBackedMap(tuple));
            assertEquals(1L,projection.getOrderCount());
        });
        var day=orders.revenueDays(java.time.OffsetDateTime.now().minusDays(1),java.time.OffsetDateTime.now().plusDays(1)).get(0);
        assertNotNull(day.getOrderCount());assertEquals(1L,day.getOrderCount());
    }
    @Test @WithMockUser(roles="USER") void userCannotAccessRevenue() throws Exception {
        mvc.perform(get("/admin/revenue")).andExpect(status().isForbidden());mvc.perform(get("/admin/revenue/data").param("from","2026-01-01").param("to","2026-01-01")).andExpect(status().isForbidden());
    }
    @Test void jwtCookieLoginLogoutRegression() throws Exception {
        var encoder=new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        new TransactionTemplate(manager).executeWithoutResult(s -> users.findByUsernameAndIsDeletedFalse("alice").orElseThrow().setPassword(encoder.encode("isolated-test-password")));
        var login=mvc.perform(post("/login").with(csrf()).param("username","alice").param("password","isolated-test-password"))
                .andExpect(status().is3xxRedirection()).andReturn();
        String cookie=login.getResponse().getHeaders("Set-Cookie").stream().filter(c->c.startsWith("SHOPTECH_AUTH=")).findFirst().orElseThrow();
        assertTrue(cookie.contains("HttpOnly"));assertTrue(cookie.contains("SameSite=Lax"));
        jakarta.servlet.http.Cookie auth=new jakarta.servlet.http.Cookie("SHOPTECH_AUTH",cookie.substring("SHOPTECH_AUTH=".length(),cookie.indexOf(';')));
        mvc.perform(get("/cart").cookie(auth)).andExpect(status().isOk());
        mvc.perform(post("/logout").cookie(auth).with(csrf())).andExpect(status().is3xxRedirection()).andExpect(header().string("Location","/"));
        mvc.perform(get("/cart")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đăng nhập")));
        mvc.perform(get("/login")).andExpect(status().isOk());
    }
    @Test @WithMockUser(roles="ADMIN") void existingAdminPagesRegression() throws Exception {
        User bob=users.findByUsernameAndIsDeletedFalse("bob").orElseThrow();
        for(String url:java.util.List.of("/admin","/admin/users","/admin/users/new",
                "/admin/users/"+bob.getId()+"/edit","/admin/users/trash",
                "/admin/categories","/admin/products"))mvc.perform(get(url)).andExpect(status().isOk());
        mvc.perform(get("/admin"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/admin/users\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/admin/users/trash\"")));
        mvc.perform(get("/admin/users"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/admin/users/trash\"")));
        mvc.perform(get("/admin/users/trash"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/admin/users\"")));
        mvc.perform(get("/products").param("keyword","Test").param("categoryId",categories.findAll().get(0).getId().toString()).param("page","0")).andExpect(status().isOk());
    }
    @Test @WithMockUser(roles="ADMIN") void adminCanCreateAndUpdateUsersThroughForms() throws Exception {
        mvc.perform(post("/admin/users/new").with(csrf())
                        .param("fullName","Created User")
                        .param("username","created-user")
                        .param("email","created@example.invalid")
                        .param("password","secret123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));
        User created=users.findByUsernameAndIsDeletedFalse("created-user").orElseThrow();

        mvc.perform(get("/admin/users/"+created.getId()+"/edit"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cập nhật tài khoản")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/admin/users/"+created.getId()+"/edit\"")));
        mvc.perform(post("/admin/users/"+created.getId()+"/edit").with(csrf())
                        .param("fullName","Updated User")
                        .param("username","created-user")
                        .param("email","created@example.invalid")
                        .param("phoneNumber","0901234567")
                        .param("address","Test address"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));
        assertEquals("Updated User",users.findById(created.getId()).orElseThrow().getFullName());
    }
    @Test @WithMockUser(roles="ADMIN") void adminCanSearchTrashAndRestoreUser() throws Exception {
        User bob=users.findByUsernameAndIsDeletedFalse("bob").orElseThrow();
        mvc.perform(post("/admin/users/"+bob.getId()+"/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        mvc.perform(get("/admin/users/trash").param("keyword","bob"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bob Test")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Alice Test"))));
        mvc.perform(get("/admin/users/"+bob.getId()+"/restore"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Xác nhận khôi phục tài khoản")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/admin/users/"+bob.getId()+"/restore\"")));
        mvc.perform(post("/admin/users/"+bob.getId()+"/restore").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/trash"));
        assertFalse(users.findById(bob.getId()).orElseThrow().getIsDeleted());
    }
    @Test @WithMockUser(roles="ADMIN") void restoreConfirmationOnlyShowsForUsersInTrash() throws Exception {
        User bob=users.findByUsernameAndIsDeletedFalse("bob").orElseThrow();
        mvc.perform(get("/admin/users/"+bob.getId()+"/restore"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/trash"));
    }
    @Test @WithMockUser(roles="ADMIN") void permanentDeleteRemovesUserWithoutOrderHistory() throws Exception {
        User bob=users.findByUsernameAndIsDeletedFalse("bob").orElseThrow();
        userService.moveToTrash(bob.getId());

        mvc.perform(post("/admin/users/"+bob.getId()+"/force-delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/trash"));
        assertFalse(users.existsById(bob.getId()));
    }
    @Test @WithMockUser(roles="ADMIN") void permanentDeletePreservesUsersWithOrderHistory() throws Exception {
        placeOrder();
        User alice=users.findByUsernameAndIsDeletedFalse("alice").orElseThrow();
        userService.moveToTrash(alice.getId());

        mvc.perform(post("/admin/users/"+alice.getId()+"/force-delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", org.hamcrest.Matchers.containsString("đơn hàng")));
        assertTrue(users.existsById(alice.getId()));
    }
    @Test @WithMockUser(username="alice") void cartApiRejectsFractionalAndNegativeQuantity() throws Exception {
        for(String quantity:java.util.List.of("0","-1","1.5"))mvc.perform(post("/api/cart/items").with(csrf()).contentType("application/json").content("{\"productId\":"+productId+",\"quantity\":"+quantity+"}")).andExpect(status().isBadRequest());
    }
    @Test void idempotentReplayDoesNotConsumeNewCart() {
        cartService.add("alice",productId,1);OrderDto.Checkout input=checkoutInput();OrderDto first=orderService.checkout("alice",input);
        cartService.add("alice",productId,2);
        assertEquals(first.id(),orderService.checkout("alice",input).id());
        assertEquals(2,cartService.get("alice").quantity());assertEquals(9,products.findById(productId).orElseThrow().getStockQuantity());assertEquals(1,orders.count());
    }
    @Test void sameRequestKeyCannotExposeAnotherOwnersOrderAndRollsBackStock() {
        OrderDto.Checkout input=checkoutInput();cartService.add("alice",productId,1);orderService.checkout("alice",input);cartService.add("bob",productId,1);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->orderService.checkout("bob",input));
        assertEquals(1,orders.count());assertEquals(9,products.findById(productId).orElseThrow().getStockQuantity());assertEquals(1,cartService.get("bob").quantity());
    }
    @Test void checkoutRefreshesProductAlreadyInPersistenceContext() throws Exception {
        cartService.add("alice",productId,1);ExecutorService pool=Executors.newSingleThreadExecutor();
        try {
            new TransactionTemplate(manager).executeWithoutResult(s->{
                Product stale=products.findById(productId).orElseThrow();assertEquals(10,stale.getStockQuantity());
                try {pool.submit(()->new TransactionTemplate(manager).executeWithoutResult(other->{Product current=products.findById(productId).orElseThrow();current.setStockQuantity(3);current.setDiscountPrice(new BigDecimal("60"));})).get(10,TimeUnit.SECONDS);}
                catch(Exception e){throw new IllegalStateException(e);}
                OrderDto placed=orderService.checkout("alice",checkoutInput());assertEquals(new BigDecimal("60.00"),placed.total());
            });
            assertEquals(2,products.findById(productId).orElseThrow().getStockQuantity());
        }finally{pool.shutdownNow();}
    }
    @Test void differentCheckoutKeysInParallelStillCreateOneOrder() throws Exception {
        cartService.add("alice",productId,1);ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        try {
            Callable<Boolean> place=()->{start.await();try{orderService.checkout("alice",checkoutInput());return true;}catch(org.springframework.web.server.ResponseStatusException e){assertEquals(409,e.getStatusCode().value());return false;}};
            Future<Boolean> a=pool.submit(place),b=pool.submit(place);start.countDown();assertNotEquals(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS));
            assertEquals(1,orders.count());assertEquals(9,products.findById(productId).orElseThrow().getStockQuantity());assertEquals(0,items.count());
        }finally{pool.shutdownNow();}
    }
    @Test @WithMockUser(username="alice") void shoppingLinksDoNotLoopBackToLoggedInLanding() throws Exception {
        var link=java.util.regex.Pattern.compile("<a[^>]*href=\"/products\"[^>]*>\\s*(?:<i[^>]*></i>\\s*)?Tiếp tục mua sắm\\s*</a>");
        for(String url:java.util.List.of("/","/account")) {
            String html=mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(link.matcher(html).find(),"Shopping CTA should lead to catalog: "+url);
        }
    }
    @Test @WithMockUser(roles="ADMIN") void confirmedCancellationAndPaidOrderProtection() {
        OrderDto order=placeOrder();sellerTransition(order.id(),new OrderDto.Transition(OrderStatus.PENDING,OrderStatus.CONFIRMED));
        jdbc.update("update orders set payment_status='PAID' where id=?",order.id());
        jdbc.update("update shop_orders set payment_status='PAID' where order_id=?",order.id());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->sellerTransition(order.id(),new OrderDto.Transition(OrderStatus.CONFIRMED,OrderStatus.CANCELLED)));
        assertEquals(9,products.findById(productId).orElseThrow().getStockQuantity());
        jdbc.update("update orders set payment_status='UNPAID' where id=?",order.id());
        jdbc.update("update shop_orders set payment_status='UNPAID' where order_id=?",order.id());
        assertEquals(OrderStatus.CANCELLED,sellerTransition(order.id(),new OrderDto.Transition(OrderStatus.CONFIRMED,OrderStatus.CANCELLED)).status());
        assertEquals(10,products.findById(productId).orElseThrow().getStockQuantity());
    }
    @Test @WithMockUser(roles="ADMIN") void cancellationRollsBackEarlierRestockWhenLaterLineFails() {
        cartService.add("alice",productId,1);
        Long second=new TransactionTemplate(manager).execute(s->{Product p=new Product();p.setShop(products.findById(productId).orElseThrow().getShop());p.setName("Second");p.setSlug("second");p.setPrice(BigDecimal.ONE);p.setStockQuantity(1);p.setCategory(categories.findAll().get(0));return products.saveAndFlush(p).getId();});
        cartService.add("alice",second,1);OrderDto order=orderService.checkout("alice",checkoutInput());
        jdbc.update("update products set stock_quantity=? where id=?",Integer.MAX_VALUE,second);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->sellerTransition(order.id(),new OrderDto.Transition(OrderStatus.PENDING,OrderStatus.CANCELLED)));
        assertEquals(9,products.findById(productId).orElseThrow().getStockQuantity());assertEquals(Integer.MAX_VALUE,products.findById(second).orElseThrow().getStockQuantity());
        assertEquals(OrderStatus.PENDING,orderService.owned("alice",order.id()).status());
    }
}
