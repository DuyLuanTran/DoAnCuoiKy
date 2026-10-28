package com.example.shoptech;
import com.example.shoptech.entity.*;
import com.example.shoptech.repository.*;
import com.example.shoptech.service.*;
import com.example.shoptech.dto.OrderDto;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.config.location=classpath:/application-test.properties","spring.datasource.url=jdbc:h2:mem:seller-orders;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc @Transactional @WithMockUser(username="alice",roles="SELLER")
class SellerOrderIntegrationTest {
 @Autowired MockMvc mvc;@Autowired UserRepository users;@Autowired ShopRepository shops;@Autowired CategoryRepository categories;@Autowired ProductRepository products;
 @Autowired ShopOrderRepository shipments;@Autowired ShopOrderHistoryRepository audits;@Autowired OrderRepository orders;
 @Autowired OrderNotificationRepository notificationRows;@Autowired StoredOrderNotificationService notifications;
 @Autowired OrderService checkout;@Autowired CartService cart;@Autowired SellerOrderService seller;@Autowired RevenueService revenue;
 Shop alice,bob;Product a,b;OrderDto parent;Long aid,bid;
 @BeforeEach void seed(){
  User au=account("alice"),bu=account("bob");account("buyer");alice=TestShops.active(shops,au,"alice");bob=TestShops.active(shops,bu,"bob");alice.setCommissionRate(new BigDecimal("10"));bob.setCommissionRate(new BigDecimal("20"));
  Category c=new Category();c.setName("Test");c.setSlug("test");categories.save(c);a=product(alice,c,"Alice product",100);b=product(bob,c,"Bob product",250);
  cart.add("buyer",a.getId(),2);cart.add("buyer",b.getId(),1);parent=checkout.checkout("buyer",input(UUID.randomUUID().toString()));
  aid=shipments.findAll().stream().filter(s->s.getShop().getId().equals(alice.getId())).findFirst().orElseThrow().getId();bid=shipments.findAll().stream().filter(s->s.getShop().getId().equals(bob.getId())).findFirst().orElseThrow().getId();
 }
 User account(String name){return users.save(User.builder().username(name).email(name+"@example.invalid").password("unused").fullName(name).build());}
 Product product(Shop shop,Category c,String name,int price){Product p=new Product();p.setShop(shop);p.setCategory(c);p.setName(name);p.setSlug(name.replace(' ','-').toLowerCase());p.setPrice(BigDecimal.valueOf(price));p.setStockQuantity(10);return products.saveAndFlush(p);}
 OrderDto.Checkout input(String key){return new OrderDto.Checkout("Buyer","0900000000","Test address",null,"COD",key);}
 void authAlice(){var context=org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();context.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("alice","unused",List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SELLER"))));org.springframework.security.core.context.SecurityContextHolder.setContext(context);}
 void change(Long id,OrderStatus expected,OrderStatus target){authAlice();seller.transition("alice",id,new OrderDto.Transition(expected,target),"Test reason");}
 void complete(){change(aid,OrderStatus.PENDING,OrderStatus.CONFIRMED);change(aid,OrderStatus.CONFIRMED,OrderStatus.PICKED_UP);change(aid,OrderStatus.PICKED_UP,OrderStatus.SHIPPING);change(aid,OrderStatus.SHIPPING,OrderStatus.COMPLETED);}
 @Test void checkoutSplitsAndSnapshotsCommissionAndIdempotency(){
  assertEquals(2,shipments.count());assertEquals(0,parent.total().compareTo(new BigDecimal("450")));assertEquals(8,a.getStockQuantity());assertEquals(9,b.getStockQuantity());
  ShopOrder s=shipments.findById(aid).orElseThrow();assertEquals(0,s.getCommissionAmount().compareTo(new BigDecimal("20")));assertEquals(0,s.getSellerAmount().compareTo(new BigDecimal("180")));assertEquals(1,s.getOrder().getDetails().stream().filter(d->d.getShopOrder().getId().equals(aid)).count());
  alice.setCommissionRate(new BigDecimal("99"));a.setPrice(new BigDecimal("999"));assertEquals(0,s.getCommissionRate().compareTo(new BigDecimal("10")));assertEquals(0,seller.detail("alice",aid).items().get(0).price().compareTo(new BigDecimal("100")));
  cart.add("buyer",a.getId(),1);String key=UUID.randomUUID().toString();OrderDto next=checkout.checkout("buyer",input(key));assertEquals(next.id(),checkout.checkout("buyer",input(key)).id());assertEquals(3,shipments.count());
 }
 @Test void persistedNotificationsAreScopedAndReturnRequestReachesSeller() throws Exception {
  assertEquals(1,notifications.inbox("buyer",0).unread());
  assertEquals(1,notifications.inbox("alice",0).unread());
  assertEquals(1,notifications.inbox("bob",0).unread());
  mvc.perform(get("/notifications").with(user("bob").roles("SELLER")))
      .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1));
  var first=notificationRows.findByRecipientUsernameOrderByCreatedAtDescIdDesc("alice",org.springframework.data.domain.PageRequest.of(0,10)).getContent().get(0);
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->notifications.open("bob",first.getId()));
  mvc.perform(post("/notifications/"+first.getId()+"/open").with(user("alice").roles("SELLER")))
      .andExpect(status().isForbidden());
  mvc.perform(post("/notifications/"+first.getId()+"/open").with(user("alice").roles("SELLER")).with(csrf()))
      .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/seller/orders/"+aid));
  assertEquals(0,notifications.inbox("alice",0).unread());
  complete();
  assertEquals(5,notifications.inbox("buyer",0).unread());
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->checkout.requestReturn("bob",parent.id(),aid,"Không đúng sản phẩm"));
  mvc.perform(post("/orders/"+parent.id()+"/shops/"+aid+"/return")
      .with(user("buyer").roles("USER")).with(csrf()).param("reason","Sản phẩm bị lỗi khi sử dụng"))
      .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/orders/"+parent.id()));
  assertEquals(OrderStatus.RETURN_REQUESTED,shipments.findById(aid).orElseThrow().getStatus());
  assertEquals(1,notifications.inbox("alice",0).unread());
  assertTrue(notifications.inbox("alice",0).items().get(0).message().contains("trả hàng"));
  mvc.perform(get("/orders/"+parent.id()).with(user("buyer").roles("USER")))
      .andExpect(status().isOk()).andExpect(content().string(containsString("Lịch sử trạng thái của shop")));
 }
 @Test void sellerPagesAndFiltersOnlyExposeOwnShipment() throws Exception {
  mvc.perform(get("/seller/orders").with(user("alice").roles("SELLER"))).andExpect(status().isOk()).andExpect(content().string(containsString("/seller/orders/"+aid))).andExpect(content().string(not(containsString("/seller/orders/"+bid))));
  mvc.perform(get("/seller/orders/"+aid).with(user("alice").roles("SELLER"))).andExpect(status().isOk()).andExpect(content().string(containsString("Alice product"))).andExpect(content().string(not(containsString("Bob product"))));
  mvc.perform(get("/seller/orders").param("status","COMPLETED").with(user("alice").roles("SELLER"))).andExpect(content().string(containsString("Không có đơn hàng")));
  authAlice();assertEquals(1,seller.search("alice","buyer",null,0).getTotalElements());
 }
 @Test void crossShopReadUpdateAndHistoryAreDenied() throws Exception {
  mvc.perform(get("/seller/orders/"+bid).with(user("alice").roles("SELLER"))).andExpect(status().isNotFound());
  mvc.perform(patch("/seller/orders/"+bid+"/status").with(user("alice").roles("SELLER")).with(csrf()).contentType("application/json").content("{\"expectedStatus\":\"PENDING\",\"status\":\"CONFIRMED\"}")).andExpect(status().isNotFound());
  authAlice();assertThrows(org.springframework.web.server.ResponseStatusException.class,()->seller.history("alice",bid));assertEquals(0,audits.count());
 }
 @Test void cancellingOneShopRestocksOnlyItsLinesAndRetryConflicts(){
  change(aid,OrderStatus.PENDING,OrderStatus.CANCELLED);assertEquals(10,a.getStockQuantity());assertEquals(9,b.getStockQuantity());assertEquals(OrderStatus.PENDING,shipments.findById(bid).orElseThrow().getStatus());assertEquals(OrderStatus.PENDING,checkout.owned("buyer",parent.id()).status());assertEquals(0,checkout.owned("buyer",parent.id()).total().compareTo(new BigDecimal("250")));
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->change(aid,OrderStatus.PENDING,OrderStatus.CANCELLED));assertEquals(10,a.getStockQuantity());assertEquals(1,audits.count());
 }
 @Test void transitionsAndRefundsAuditAndRestockOnce(){
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->change(aid,OrderStatus.PENDING,OrderStatus.COMPLETED));complete();assertEquals("PAID",seller.detail("alice",aid).paymentStatus());assertEquals("PARTIALLY_PAID",checkout.owned("buyer",parent.id()).paymentStatus());
  change(aid,OrderStatus.COMPLETED,OrderStatus.RETURN_REQUESTED);assertEquals(8,a.getStockQuantity());change(aid,OrderStatus.RETURN_REQUESTED,OrderStatus.REFUNDED);assertEquals(10,a.getStockQuantity());assertEquals("REFUNDED",seller.detail("alice",aid).paymentStatus());assertEquals(6,audits.count());
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->change(aid,OrderStatus.RETURN_REQUESTED,OrderStatus.REFUNDED));assertEquals(10,a.getStockQuantity());
 }
 @Test void revenueIsScopedUsesSnapshotAndExcludesReturns() throws Exception {
  complete();LocalDate today=LocalDate.now(RevenueService.ZONE);var r=revenue.sellerReport("alice",today,today,RevenueService.Group.DAY);assertEquals(1,r.completedOrders());assertEquals(0,r.totalRevenue().compareTo(new BigDecimal("200")));assertEquals(0,r.totalCommission().compareTo(new BigDecimal("20")));assertEquals(0,r.totalSellerAmount().compareTo(new BigDecimal("180")));
  mvc.perform(get("/seller/revenue/data").param("from",today.toString()).param("to",today.toString()).param("shopId",bob.getId().toString()).with(user("alice").roles("SELLER"))).andExpect(status().isOk()).andExpect(jsonPath("$.totalRevenue").value(200));
  mvc.perform(get("/seller/revenue").with(user("alice").roles("SELLER"))).andExpect(status().isOk());
  change(aid,OrderStatus.COMPLETED,OrderStatus.RETURN_REQUESTED);assertEquals(0,revenue.sellerReport("alice",today,today,RevenueService.Group.DAY).completedOrders());
 }
 @Test void suspendedShopCanFulfilExistingOrdersButCannotSellNew(){
  alice.setStatus(ShopStatus.SUSPENDED);shops.flush();change(aid,OrderStatus.PENDING,OrderStatus.CONFIRMED);assertThrows(org.springframework.web.server.ResponseStatusException.class,()->cart.add("buyer",a.getId(),1));
 }
 @Test void adminIsReadOnlyAndSidebarUsesMatchingIcons() throws Exception {
  mvc.perform(get("/admin/orders/"+parent.id()).with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(content().string(containsString("fa-clipboard-list"))).andExpect(content().string(containsString("fa-chart-line"))).andExpect(content().string(not(containsString("id=\"order-status-form\""))));
  mvc.perform(patch("/admin/orders/"+parent.id()+"/status").with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content("{\"expectedStatus\":\"PENDING\",\"status\":\"CONFIRMED\"}")).andExpect(status().isForbidden());
  mvc.perform(get("/seller/orders").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
 }
 @Test void userPermissionsAndCsrfAndRequiredReason() throws Exception {
  mvc.perform(get("/seller/orders").with(user("buyer").roles("USER"))).andExpect(status().isForbidden());
  mvc.perform(patch("/seller/orders/"+aid+"/status").with(user("alice").roles("SELLER")).contentType("application/json").content("{\"expectedStatus\":\"PENDING\",\"status\":\"CONFIRMED\"}")).andExpect(status().isForbidden());
  mvc.perform(patch("/seller/orders/"+aid+"/status").with(user("alice").roles("SELLER")).with(csrf()).contentType("application/json").content("{\"expectedStatus\":\"PENDING\",\"status\":\"CANCELLED\"}"))
      .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("Vui lòng nhập lý do hủy hoặc trả hàng / hoàn tiền."));assertEquals(8,a.getStockQuantity());
 }
 @Test void buyerSeesBothShopStatusesAndAnotherBuyerCannotRead() throws Exception {
  change(aid,OrderStatus.PENDING,OrderStatus.CONFIRMED);
  mvc.perform(get("/orders/"+parent.id()).with(user("buyer").roles("USER"))).andExpect(status().isOk()).andExpect(content().string(containsString("Test shop alice"))).andExpect(content().string(containsString("Test shop bob"))).andExpect(content().string(containsString("CONFIRMED")));
  mvc.perform(get("/orders/"+parent.id()).with(user("bob").roles("USER"))).andExpect(status().isNotFound());
 }
 @Test void invalidSecondShopRollsBackEntireCheckout(){
  cart.add("buyer",a.getId(),1);cart.add("buyer",b.getId(),1);b.setStockQuantity(0);products.flush();
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->checkout.checkout("buyer",input(UUID.randomUUID().toString())));
  // Refresh after the failed nested transaction: no persisted partial checkout exists.
  assertEquals(1,orders.count());assertEquals(2,shipments.count());
 }
 @Test void historicalMismatchCannotRestockUntilReconciled(){
  ShopOrder s=shipments.findById(aid).orElseThrow();s.setHistoricalImport(true);s.setSubtotal(new BigDecimal("1"));s.setTotal(new BigDecimal("1"));s.setCommissionAmount(new BigDecimal("0.10"));s.setSellerAmount(new BigDecimal("0.90"));shipments.flush();
  assertTrue(seller.needsReconciliation(s));assertThrows(org.springframework.web.server.ResponseStatusException.class,()->change(aid,OrderStatus.PENDING,OrderStatus.CANCELLED));assertEquals(8,a.getStockQuantity());assertEquals(0,audits.count());
 }

}
