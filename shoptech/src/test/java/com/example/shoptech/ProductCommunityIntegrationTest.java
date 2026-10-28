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

@SpringBootTest(properties={"spring.config.location=classpath:/application-test.properties","spring.datasource.url=jdbc:h2:mem:product-community;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc @Transactional @WithMockUser(username="alice",roles="SELLER")
class ProductCommunityIntegrationTest {
 @Autowired MockMvc mvc;@Autowired UserRepository users;@Autowired ShopRepository shops;@Autowired CategoryRepository categories;@Autowired ProductRepository products;
 @Autowired ShopOrderRepository shipments;@Autowired ShopOrderHistoryRepository audits;@Autowired OrderRepository orders;
 @Autowired OrderService checkout;@Autowired CartService cart;@Autowired SellerOrderService seller;@Autowired RevenueService revenue;
 @Autowired UserService userService; @Autowired ProductCommunityService community; @Autowired ProductReviewRepository reviews; @Autowired ProductFavoriteRepository favorites; @Autowired jakarta.persistence.EntityManager em;
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

 static final String TEXT="Sản phẩm sử dụng ổn định, đóng gói cẩn thận và phù hợp với nhu cầu học tập của tôi.";
 void auth(String name,String role){var context=org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();context.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(name,"unused",List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_"+role))));org.springframework.security.core.context.SecurityContextHolder.setContext(context);}
 void buyer(){auth("buyer","USER");}
 ProductCommunityService.ReviewView review(){complete();buyer();return community.create("buyer",a.getId(),5,TEXT,List.of());}
 @Test void favoritesArePerAccountIdempotentAndVisibilityAware() {
  buyer();assertTrue(community.favorite("buyer",a.getId(),true));assertTrue(community.favorite("buyer",a.getId(),true));assertEquals(1,favorites.count());assertEquals(Set.of(a.getId()),community.selected("buyer",List.of(a.getId(),b.getId())));assertEquals(1,community.favorites("buyer",0).getTotalElements());
  auth("bob","USER");assertTrue(community.selected("bob",List.of(a.getId())).isEmpty());assertEquals(0,community.favorites("bob",0).getTotalElements());
  a.setIsBlocked(true);products.flush();buyer();assertEquals(0,community.favorites("buyer",0).getTotalElements());assertFalse(community.favorite("buyer",a.getId(),false));assertEquals(0,favorites.count());
 }
 @Test void deletingAnUnusedAccountAlsoRemovesItsFavorites() {
  User liker=account("liker");auth("liker","USER");community.favorite("liker",a.getId(),true);assertEquals(1,favorites.count());liker.setIsDeleted(true);users.flush();auth("alice","ADMIN");userService.forceDelete(liker.getId());users.flush();em.clear();assertEquals(0,favorites.count());
 }
 @Test void pendingOrderCannotReviewAndOtherBuyersCannotReview() {
  buyer();assertFalse(community.eligibility("buyer",a.getId()).allowed());assertThrows(org.springframework.web.server.ResponseStatusException.class,()->community.create("buyer",a.getId(),5,TEXT,List.of()));
  complete();auth("bob","USER");assertThrows(org.springframework.web.server.ResponseStatusException.class,()->community.create("bob",a.getId(),5,TEXT,List.of()));assertEquals(0,reviews.count());
 }
 @Test void completedShopOrderAllowsReviewWhileParentStillPendingAndBlocksDuplicate() {
  var r=review();assertEquals(OrderStatus.PENDING,orders.findById(parent.id()).orElseThrow().getOrderStatus());assertEquals(5,r.rating());assertEquals(1,community.publicReviews(a.getId(),0).getTotalElements());assertFalse(community.eligibility("buyer",a.getId()).allowed());assertEquals(5,community.summaries(List.of(a.getId())).get(a.getId()).average());
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->community.create("buyer",a.getId(),4,TEXT,List.of()));assertEquals(1,reviews.count());
 }
 @Test void invalidRatingAndShortTextRejectedByApi() throws Exception {
  complete();
  mvc.perform(multipart("/api/reviews/products/"+a.getId()).param("rating","6").param("content",TEXT).with(user("buyer").roles("USER")).with(csrf())).andExpect(status().isBadRequest());
  mvc.perform(multipart("/api/reviews/products/"+a.getId()).param("rating","5").param("content","ngắn").with(user("buyer").roles("USER")).with(csrf())).andExpect(status().isBadRequest());assertEquals(0,reviews.count());
 }
 @Test void spoofedMediaRejectedBeforePersistence() throws Exception {
  complete();var file=new org.springframework.mock.web.MockMultipartFile("files","fake.png","image/png","<script>bad</script>".getBytes());
  mvc.perform(multipart("/api/reviews/products/"+a.getId()).file(file).param("rating","5").param("content",TEXT).with(user("buyer").roles("USER")).with(csrf())).andExpect(status().isBadRequest());assertEquals(0,reviews.count());
 }
 @Test void csrfAndAnonymousAndRoleBoundaries() throws Exception {
  mvc.perform(put("/api/favorites/"+a.getId()).with(user("buyer").roles("USER"))).andExpect(status().isForbidden());
  mvc.perform(put("/api/favorites/"+a.getId()).with(csrf())).andExpect(status().isOk()); // default alice is authenticated
  mvc.perform(get("/api/favorites/selected").param("ids",a.getId().toString()).with(anonymous())).andExpect(status().isUnauthorized());
  mvc.perform(get("/admin/reviews").with(user("buyer").roles("USER"))).andExpect(status().isForbidden());
  mvc.perform(get("/seller/reviews").with(user("buyer").roles("USER"))).andExpect(status().isForbidden());
 }
 @Test void sellerRepliesOnlyToOwnShopAndVersionIsChecked() throws Exception {
  var r=review();authAlice();community.reply("alice",r.id(),r.version(),"Cảm ơn bạn đã tin tưởng shop!");assertEquals("Cảm ơn bạn đã tin tưởng shop!",community.publicReviews(a.getId(),0).getContent().get(0).sellerReply());
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->community.reply("alice",r.id(),r.version(),"stale"));
  auth("bob","SELLER");assertEquals(0,community.manage("bob",true,0).getTotalElements());assertThrows(org.springframework.web.server.ResponseStatusException.class,()->community.reply("bob",r.id(),1,"Không được phép"));
  mvc.perform(get("/seller/reviews").with(user("alice").roles("SELLER"))).andExpect(status().isOk()).andExpect(content().string(containsString("Đánh giá sản phẩm của shop")));
 }
 @Test void adminModerationRemovesReviewAndStarsAndAuditsEveryChange() throws Exception {
  var r=review();account("admin");auth("admin","ADMIN");community.moderate("admin",r.id(),r.version(),true,"Nội dung cần kiểm tra");assertEquals(0,community.publicReviews(a.getId(),0).getTotalElements());assertTrue(community.summaries(List.of(a.getId())).isEmpty());assertEquals(1,em.createQuery("select count(h) from ReviewModerationHistory h",Long.class).getSingleResult());
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->community.moderate("admin",r.id(),r.version(),false,"stale request"));var hidden=reviews.findById(r.id()).orElseThrow();community.moderate("admin",r.id(),hidden.getVersion(),false,"Đã xem xét hợp lệ");assertEquals(1,community.publicReviews(a.getId(),0).getTotalElements());assertEquals(2,em.createQuery("select count(h) from ReviewModerationHistory h",Long.class).getSingleResult());
  mvc.perform(get("/admin/reviews").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(content().string(containsString("Quản lý đánh giá sản phẩm")));
 }
 @Test void refundedOrderCannotSubmitNewReview() {
  complete();change(aid,OrderStatus.COMPLETED,OrderStatus.RETURN_REQUESTED);change(aid,OrderStatus.RETURN_REQUESTED,OrderStatus.REFUNDED);buyer();assertFalse(community.eligibility("buyer",a.getId()).allowed());assertThrows(org.springframework.web.server.ResponseStatusException.class,()->community.create("buyer",a.getId(),5,TEXT,List.of()));
 }
 @Test void deletedBuyerAndOwnShopCannotReviewOrFavorite() {
  complete();authAlice();assertThrows(org.springframework.web.server.ResponseStatusException.class,()->community.create("alice",a.getId(),5,TEXT,List.of()));users.findByUsernameAndIsDeletedFalse("buyer").orElseThrow().setIsActive(false);users.flush();buyer();assertThrows(org.springframework.web.server.ResponseStatusException.class,()->community.favorite("buyer",a.getId(),true));assertThrows(org.springframework.web.server.ResponseStatusException.class,()->community.create("buyer",a.getId(),5,TEXT,List.of()));
 }
 @Test void publicProductAndFavoritesRenderSharedHeaderWithoutPrivateContact() throws Exception {
  mvc.perform(get("/products/"+a.getSlug()).with(anonymous())).andExpect(status().isOk()).andExpect(content().string(containsString("data-reviews-product"))).andExpect(content().string(containsString("review-content")));
  buyer();community.favorite("buyer",a.getId(),true);
  mvc.perform(get("/favorites").with(user("buyer").roles("USER"))).andExpect(status().isOk()).andExpect(content().string(containsString("Alice product"))).andExpect(content().string(containsString("ShopTech")));
 }
 @Test void publicReviewPaginationAndTextAreReturnedAsData() {
  var r=review();buyer();assertEquals(1,community.publicReviews(a.getId(),0).getTotalElements());assertEquals(TEXT,community.publicReviews(a.getId(),0).getContent().get(0).content());a.setIsDeleted(true);products.flush();assertThrows(org.springframework.web.server.ResponseStatusException.class,()->community.publicReviews(a.getId(),0));assertTrue(community.summaries(List.of(a.getId())).isEmpty());
 }
}
