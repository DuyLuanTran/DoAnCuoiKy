package com.example.shoptech;

import com.example.shoptech.dto.PublicShop;
import com.example.shoptech.entity.*;
import com.example.shoptech.repository.*;
import com.example.shoptech.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties={"spring.config.location=classpath:/application-test.properties","spring.datasource.url=jdbc:h2:mem:public-shops;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc @Transactional
class PublicShopIntegrationTest {
 @Autowired MockMvc mvc;@Autowired ShopRepository shops;@Autowired UserRepository users;@Autowired ProductRepository products;@Autowired CategoryRepository categories;@Autowired PublicShopService publicShops;
 Shop alice,bob,empty,pending,suspended;Category category;Product a,b,a2;
 @BeforeEach void seed(){
  alice=shop("alice");bob=shop("bob");empty=shop("empty");pending=shop("pending");pending.setStatus(ShopStatus.PENDING);suspended=shop("suspended");suspended.setStatus(ShopStatus.SUSPENDED);shops.flush();
  category=new Category();category.setName("Laptops");category.setSlug("laptops");categories.save(category);
  a=product(alice,"Laptop Alice One",100);a2=product(alice,"Laptop Alice Two",150);a2.setDiscountPrice(new BigDecimal("80"));a2.setStockQuantity(0);b=product(bob,"Laptop Bob",50);products.flush();
 }
 Shop shop(String name){User u=users.save(User.builder().username(name).email(name+"-private@example.invalid").password("private-hash").fullName("Private owner "+name).build());Shop s=TestShops.active(shops,u,name);s.setName("Cửa hàng "+name);s.setDescription("Giới thiệu "+name);s.setAddress("PRIVATE APPLICATION ADDRESS");s.setPhone("0912345678");return shops.saveAndFlush(s);}
 Product product(Shop owner,String name,int price){Product p=new Product();p.setShop(owner);p.setCategory(category);p.setName(name);p.setSlug(name.toLowerCase().replace(' ','-'));p.setPrice(BigDecimal.valueOf(price));p.setStockQuantity(4);p.setMainImage("/user/img/product01.png");return products.saveAndFlush(p);}
 MvcResult page(String path) throws Exception{return mvc.perform(get(path)).andExpect(status().isOk()).andReturn();}
 @SuppressWarnings("unchecked") Page<Product> result(MvcResult r){return (Page<Product>)r.getModelAndView().getModel().get("products");}
 String html(MvcResult r){return new String(r.getResponse().getContentAsByteArray(),StandardCharsets.UTF_8);}
 @Test void guestShopPageOnlyShowsOwnProductsAndPublicInformation() throws Exception{
  MvcResult r=page("/shops/alice");assertEquals(2,result(r).getTotalElements());assertEquals(2L,r.getModelAndView().getModel().get("shopProductCount"));assertTrue(html(r).contains("Cửa hàng alice"));assertTrue(html(r).contains("Laptop Alice One"));assertFalse(html(r).contains("Laptop Bob"));
  for(String secret:List.of("alice-private@example.invalid","PRIVATE APPLICATION ADDRESS","0912345678","Private owner alice","private-hash"))assertFalse(html(r).contains(secret),secret);
  assertTrue(html(r).contains("src=\"/user/img/product01.png\""));assertTrue(publicShops.bySlug("alice") instanceof PublicShop);
 }
 @Test void globalCatalogCanFilterAndClearShop() throws Exception{
  assertEquals(3,result(page("/products")).getTotalElements());MvcResult filtered=page("/products?shopId="+alice.getId());assertEquals(2,result(filtered).getTotalElements());assertTrue(html(filtered).contains("Xem shop"));assertFalse(html(filtered).contains("Laptop Bob"));assertTrue(html(filtered).contains("class=\"catalog-reset\" href=\"/products\""));
  assertEquals(3,publicShops.options().size());
 }
 @Test void shopRouteIgnoresForgedQueryShopId() throws Exception{
  MvcResult r=page("/shops/alice?shopId="+bob.getId());assertEquals(alice.getId(),r.getModelAndView().getModel().get("shopId"));assertEquals(2,result(r).getTotalElements());assertTrue(result(r).getContent().stream().allMatch(p->p.getShop().getId().equals(alice.getId())));
 }
 @Test void unavailableShopsAreNotPublicEvenForAdmin() throws Exception{
  for(Shop s:List.of(pending,suspended)){
   mvc.perform(get("/shops/"+s.getSlug())).andExpect(status().isNotFound());mvc.perform(get("/products").param("shopId",s.getId().toString()).with(user("admin").roles("ADMIN"))).andExpect(status().isNotFound());
  }
  mvc.perform(get("/shops/missing")).andExpect(status().isNotFound());mvc.perform(get("/products?shopId=999999")).andExpect(status().isNotFound());
 }
 @Test void inactiveAndDeletedOwnersHideTheirShopAndProducts() throws Exception{
  alice.getOwner().setIsActive(false);bob.getOwner().setIsDeleted(true);users.flush();assertEquals(1,publicShops.options().size());mvc.perform(get("/shops/alice")).andExpect(status().isNotFound());mvc.perform(get("/shops/bob")).andExpect(status().isNotFound());assertEquals(0,result(page("/products")).getTotalElements());mvc.perform(get("/products/"+a.getSlug())).andExpect(status().isNotFound());
 }
 @Test void visibilityAndCountExcludeModeratedDeletedDisabledProductsAndCategories() throws Exception{
  Product blocked=product(alice,"Blocked product",100);blocked.setIsBlocked(true);Product deleted=product(alice,"Deleted product",100);deleted.setIsDeleted(true);Product inactive=product(alice,"Inactive product",100);inactive.setIsActive(false);
  Category hidden=new Category();hidden.setName("Hidden");hidden.setSlug("hidden");hidden.setIsActive(false);categories.save(hidden);Product h=product(alice,"Hidden category product",100);h.setCategory(hidden);products.flush();
  MvcResult r=page("/shops/alice");assertEquals(2,result(r).getTotalElements());assertEquals(2L,r.getModelAndView().getModel().get("shopProductCount"));
  for(Product p:List.of(blocked,deleted,inactive,h))assertFalse(html(r).contains(p.getName()));
 }
 @Test void priceSaleStockAndSortingComposeWithinShop() throws Exception{
  assertEquals(a2.getId(),result(page("/shops/alice?sort=priceAsc")).getContent().get(0).getId());assertEquals(1,result(page("/shops/alice?minPrice=70&maxPrice=90&onSale=true")).getTotalElements());assertEquals(0,result(page("/shops/alice?minPrice=70&maxPrice=90&onSale=true&inStock=true")).getTotalElements());
  assertEquals(1,result(page("/products?shopId="+alice.getId()+"&minPrice=90&maxPrice=110&inStock=true")).getTotalElements());
 }
 @Test void paginationAndNavigationPreserveShopAndOtherFilters() throws Exception{
  for(int i=0;i<14;i++)product(alice,"Laptop Extra "+i,100+i);products.flush();
  MvcResult r=page("/shops/alice?keyword=Laptop&minPrice=50&maxPrice=200&inStock=true&sort=priceAsc&size=12");assertEquals(15,result(r).getTotalElements());assertEquals(12,result(r).getNumberOfElements());String h=html(r);assertTrue(h.contains("/shops/alice?shopId="+alice.getId()));assertTrue(h.contains("page=1"));assertTrue(h.contains("minPrice=50"));assertTrue(h.contains("sort=priceAsc"));assertTrue(h.contains("action=\"/shops/alice\""));assertTrue(h.contains("class=\"catalog-reset\" href=\"/shops/alice\""));assertFalse(h.contains("id=\"shopId\""));
  assertEquals(1,result(page("/shops/alice?page=999")).getNumber());assertEquals(0,result(page("/products?shopId="+bob.getId()+"&page=999")).getNumber());
 }
 @Test void malformedFiltersHaveControlledErrorsAndDoNotBroadenShop() throws Exception{
  MvcResult r=page("/shops/alice?minPrice=200&maxPrice=10");assertTrue(result(r).isEmpty());assertNotNull(r.getModelAndView().getModel().get("filterError"));assertEquals(alice.getId(),r.getModelAndView().getModel().get("shopId"));
  mvc.perform(get("/products?shopId=abc")).andExpect(status().isBadRequest());mvc.perform(get("/shops/alice?minPrice=abc")).andExpect(status().isBadRequest());mvc.perform(get("/products").param("keyword","a".repeat(101))).andExpect(status().isBadRequest());assertEquals(12,result(page("/shops/alice?size=999&sort=bad&page=-2")).getSize());
 }
 @Test void keywordWildcardsAreLiteralAndLongShopTextIsEscaped() throws Exception{
  alice.setName("Cửa hàng <script>alert(1)</script> "+"Dài ".repeat(15));alice.setDescription("Mô tả <img src=x onerror=alert(1)>");shops.flush();MvcResult r=page("/shops/alice");assertFalse(html(r).contains("<script>alert(1)</script>"));assertTrue(html(r).contains("&lt;script&gt;"));assertTrue(html(r).contains("marketplace.css"));assertTrue(result(page("/shops/alice?keyword=%25")).isEmpty());
 }
 @Test void emptyShopHasHonestEmptyStateAndProductLinksLeadToOwner() throws Exception{
  MvcResult r=page("/shops/empty");assertEquals(0L,r.getModelAndView().getModel().get("shopProductCount"));assertTrue(html(r).contains("Không tìm thấy sản phẩm phù hợp"));
  String catalog=html(page("/products"));assertTrue(catalog.contains("href=\"/shops/alice\""));assertTrue(catalog.contains("href=\"/shops/bob\""));assertTrue(html(page("/products/"+a.getSlug())).contains("href=\"/shops/alice\""));
 }
 @Test void sharedHeadersStillRenderOnAccountCartAndCheckout() throws Exception{
  for(String url:List.of("/account","/cart","/checkout","/orders"))mvc.perform(get(url).with(user("alice").roles("USER"))).andExpect(status().isOk()).andExpect(content().string(containsString("ShopTech")));
 }
}
