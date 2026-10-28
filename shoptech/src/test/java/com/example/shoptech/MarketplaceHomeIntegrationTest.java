package com.example.shoptech;

import com.example.shoptech.entity.*;
import com.example.shoptech.entity.Order;
import com.example.shoptech.repository.*;
import com.example.shoptech.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.config.location=classpath:/application-test.properties","spring.datasource.url=jdbc:h2:mem:market-home;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc @Transactional
class MarketplaceHomeIntegrationTest {
 @Autowired MockMvc mvc;@Autowired ProductRepository products;@Autowired CategoryRepository categories;@Autowired ShopRepository shops;@Autowired UserRepository users;@Autowired OrderRepository orders;@Autowired ShopOrderRepository shipments;@Autowired ProductSalesService sales;@Autowired ProductCatalogService catalog;
 Shop alice,bob;User buyer;Category category;Product a,b,c;
 @BeforeEach void seed(){alice=TestShops.active(shops,account("alice"),"alice");bob=TestShops.active(shops,account("bob"),"bob");buyer=account("buyer");category=new Category();category.setName("Laptop");category.setSlug("laptop");categories.save(category);a=product(alice,"Laptop Alpha");b=product(bob,"Laptop Beta");c=product(alice,"Laptop New");}
 User account(String name){return users.saveAndFlush(User.builder().username(name).fullName("Private owner "+name).email(name+"-private@example.invalid").password("unused").build());}
 Product product(Shop shop,String name){Product p=new Product();p.setName(name);p.setSlug(name.toLowerCase().replace(' ','-'));p.setShop(shop);p.setCategory(category);p.setPrice(BigDecimal.TEN);p.setStockQuantity(100);p.setMainImage("/user/img/product01.png");return products.saveAndFlush(p);}
 ShopOrder sale(Product product,int quantity,OrderStatus status,boolean deleted,boolean historical,boolean mismatch){
  BigDecimal lineAmount=product.getPrice().multiply(BigDecimal.valueOf(quantity));BigDecimal amount=mismatch?lineAmount.add(BigDecimal.ONE):lineAmount;
  Order order=new Order();order.setUser(buyer);order.setOrderCode("HOME-"+UUID.randomUUID().toString().substring(0,20));order.setRecipientName("Buyer");order.setRecipientPhone("0900000000");order.setShippingAddress("Private shipping address");order.setSubtotalAmount(amount);order.setTotalAmount(amount);order.setIsDeleted(deleted);order.setOrderStatus(status);
  ShopOrder shipment=new ShopOrder();shipment.setOrder(order);shipment.setShop(product.getShop());shipment.setShopName(product.getShop().getName());shipment.setSubtotal(amount);shipment.setTotal(amount);shipment.setCommissionRate(BigDecimal.ZERO);shipment.setCommissionAmount(BigDecimal.ZERO);shipment.setSellerAmount(amount);shipment.setStatus(status);shipment.setHistoricalImport(historical);order.getShopOrders().add(shipment);
  OrderDetail line=new OrderDetail();line.setOrder(order);line.setShopOrder(shipment);line.setProductId(product.getId());line.setProductName(product.getName());line.setPrice(product.getPrice());line.setQuantity(quantity);line.setSubtotal(lineAmount);order.getDetails().add(line);orders.saveAndFlush(order);return shipment;
 }
 MvcResult page(String path)throws Exception{return mvc.perform(get(path)).andExpect(status().isOk()).andReturn();}
 @SuppressWarnings("unchecked") Page<Product> result(MvcResult r,String attribute){return (Page<Product>)r.getModelAndView().getModel().get(attribute);}
 String html(MvcResult r){return new String(r.getResponse().getContentAsByteArray(),StandardCharsets.UTF_8);}
 @Test void guestHomepageUsesRealCatalogAndNoTemplateDemoData()throws Exception{
  MvcResult r=page("/");assertEquals(3,result(r,"products").getTotalElements());assertEquals(20,result(r,"products").getSize());String h=html(r);assertTrue(h.contains("Laptop New"));assertTrue(h.contains("/shops/alice"));assertTrue(h.contains("/user/img/product01.png"));assertFalse(h.contains("1734 Stonecoal"));assertFalse(h.contains("NEWSLETTER"));assertFalse(h.contains("PRODUCT NAME GOES HERE"));assertFalse(h.contains("Private shipping address"));
 }
 @Test void authenticatedRolesCanVisitSamePublicHomepage()throws Exception{
  for(String role:List.of("USER","SELLER","ADMIN"))mvc.perform(get("/").with(user("alice").roles(role))).andExpect(status().isOk()).andExpect(view().name("user/index"));mvc.perform(get("/user/home").with(user("buyer").roles("USER"))).andExpect(redirectedUrl("/"));
 }
 @Test void newestIsOrderedByRealCreationAndStableId()throws Exception{assertEquals(c.getId(),result(page("/"),"products").getContent().get(0).getId());}
 @Test void completedQuantityNotNumberOfOrdersDeterminesRanking()throws Exception{
  sale(a,6,OrderStatus.COMPLETED,false,false,false);sale(a,6,OrderStatus.COMPLETED,false,false,false);sale(b,11,OrderStatus.COMPLETED,false,false,false);sale(c,9,OrderStatus.PENDING,false,false,false);
  MvcResult r=page("/?feed=bestSelling");assertEquals(List.of(a.getId(),b.getId()),result(r,"products").getContent().stream().map(Product::getId).toList());assertEquals(12L,sales.counts(List.of(a.getId())).get(a.getId()));assertTrue(html(r).contains("Đã bán 12"));
 }
 @Test void strictlyMoreThanTenAndPaginationCountUseSamePredicate()throws Exception{
  sale(a,10,OrderStatus.COMPLETED,false,false,false);sale(b,11,OrderStatus.COMPLETED,false,false,false);assertEquals(List.of(b.getId()),result(page("/"),"popularProducts").getContent().stream().map(Product::getId).toList());MvcResult r=page("/products?sort=bestSelling&minSold=11&size=20");assertEquals(1,result(r,"products").getTotalElements());assertEquals(b.getId(),result(r,"products").getContent().get(0).getId());
 }
 @Test void pendingCancelledReturnedRefundedAndDeletedOrdersDoNotIncreaseSales(){
  for(OrderStatus status:List.of(OrderStatus.PENDING,OrderStatus.CONFIRMED,OrderStatus.PICKED_UP,OrderStatus.SHIPPING,OrderStatus.CANCELLED,OrderStatus.RETURN_REQUESTED,OrderStatus.REFUNDED))sale(a,30,status,false,false,false);
  sale(a,30,OrderStatus.COMPLETED,true,false,false);sale(a,2,OrderStatus.COMPLETED,false,false,false);assertEquals(2L,sales.counts(List.of(a.getId())).get(a.getId()));
 }
 @Test void historicalMismatchIsExcludedButConsistentHistoryIsCounted(){sale(a,50,OrderStatus.COMPLETED,false,true,true);sale(a,3,OrderStatus.COMPLETED,false,true,false);assertEquals(3L,sales.counts(List.of(a.getId())).get(a.getId()));}
 @Test void completedShopCountsEvenWhileSiblingParentIsPending(){ShopOrder s=sale(a,12,OrderStatus.COMPLETED,false,false,false);s.getOrder().setOrderStatus(OrderStatus.PENDING);orders.flush();assertEquals(12L,sales.counts(List.of(a.getId())).get(a.getId()));}
 @Test void shopAndProductVisibilityApplyToEveryHomepageSection()throws Exception{
  sale(a,20,OrderStatus.COMPLETED,false,false,false);sale(b,20,OrderStatus.COMPLETED,false,false,false);a.setIsBlocked(true);bob.setStatus(ShopStatus.SUSPENDED);products.flush();shops.flush();MvcResult r=page("/?feed=bestSelling");assertTrue(result(r,"products").isEmpty());assertTrue(result(r,"popularProducts").isEmpty());String h=html(r);assertFalse(h.contains("Laptop Alpha"));assertFalse(h.contains("Laptop Beta"));
 }
 @Test void emptyRankingsHaveHonestMessages()throws Exception{assertTrue(html(page("/")).contains("Chưa có sản phẩm có trên 10"));assertTrue(html(page("/?feed=bestSelling")).contains("Chưa có lượt mua hoàn tất để xếp hạng"));}
 @Test void twentyProductsPerPageAndFeedArePreserved()throws Exception{
  for(int i=0;i<21;i++)sale(product(alice,"Extra "+i),i+1,OrderStatus.COMPLETED,false,false,false);MvcResult first=page("/?feed=bestSelling");assertEquals(20,result(first,"products").getNumberOfElements());assertTrue(html(first).contains("feed=bestSelling&amp;page=1"));assertEquals(1,result(page("/?feed=bestSelling&page=99"),"products").getNumber());
 }
 @Test void publicDirectorySearchPagingAndPrivacy()throws Exception{
  for(int i=0;i<12;i++)TestShops.active(shops,account("extra"+i),"extra"+i);String h=html(page("/shops"));assertTrue(h.contains("page=1"));assertFalse(h.contains("alice-private@example.invalid"));MvcResult r=page("/shops?keyword=alice");assertTrue(html(r).contains("Test shop alice"));assertFalse(html(r).contains("Test shop bob"));assertTrue(html(page("/shops?keyword=%25")).contains("Không tìm thấy shop phù hợp"));
 }
 @Test void shopFiltersAndSalesThresholdRemainScoped()throws Exception{
  sale(a,12,OrderStatus.COMPLETED,false,false,false);sale(b,15,OrderStatus.COMPLETED,false,false,false);MvcResult r=page("/shops/alice?sort=bestSelling&minSold=11&size=20");assertEquals(1,result(r,"products").getTotalElements());assertEquals(a.getId(),result(r,"products").getContent().get(0).getId());assertTrue(html(r).contains("name=\"minSold\" value=\"11\""));
 }
 @Test void sellerDashboardHasNoDuplicateButtonsAndMenuPreviewIsConsistent()throws Exception{
  MvcResult r=mvc.perform(get("/seller").with(user("alice").roles("SELLER"))).andExpect(status().isOk()).andReturn();String h=html(r);assertFalse(h.contains("class=\"btn btn-primary mb-2\""));assertTrue(h.contains("fa-fw fa-external-link-alt"));assertEquals(1,h.split("Xem trang shop",-1).length-1);assertTrue(h.contains("href=\"/shops/alice\""));
  alice.setStatus(ShopStatus.SUSPENDED);shops.flush();mvc.perform(get("/seller").with(user("alice").roles("SELLER"))).andExpect(content().string(not(containsString("Xem trang shop"))));
 }
}
