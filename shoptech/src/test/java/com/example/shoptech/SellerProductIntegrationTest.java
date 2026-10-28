package com.example.shoptech;
import com.example.shoptech.entity.*;
import com.example.shoptech.repository.*;
import com.example.shoptech.service.CloudinaryService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties={"spring.config.location=classpath:/application-test.properties","spring.datasource.url=jdbc:h2:mem:seller-product-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc @Transactional
class SellerProductIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ProductRepository products; @Autowired CategoryRepository categories;
    @Autowired ShopRepository shops; @Autowired UserRepository users; @Autowired RoleRepository roles;
    @Autowired ProductModerationHistoryRepository history;
    @MockitoBean CloudinaryService images;
    Shop alice,bob; Category category; Product a,b;
    @BeforeEach void seed(){
        Role seller=roles.save(new Role(null,"ROLE_SELLER","Seller")),admin=roles.save(new Role(null,"ROLE_ADMIN","Admin")),buyer=roles.save(new Role(null,"ROLE_USER","Buyer"));
        User au=account("alice",seller),bu=account("bob",seller);account("admin",admin);account("buyer",buyer);
        alice=TestShops.active(shops,au,"alice");bob=TestShops.active(shops,bu,"bob");
        category=new Category();category.setName("Laptop");category.setSlug("laptops");categories.saveAndFlush(category);
        a=product(alice,"Laptop Alice","ALICE-1");b=product(bob,"Laptop Bob","BOB-1");
        when(images.uploadImage(any())).thenReturn(new CloudinaryService.UploadedImage("/user/img/product01.png","mock/image"));
    }
    User account(String name,Role role){return users.saveAndFlush(User.builder().username(name).email(name+"@example.invalid").fullName(name).password("unused").roles(new HashSet<>(Set.of(role))).build());}
    Product product(Shop owner,String name,String sku){Product p=new Product();p.setShop(owner);p.setCategory(category);p.setName(name);p.setSku(sku);p.setSlug(name.toLowerCase().replace(' ','-'));p.setPrice(new BigDecimal("100"));p.setStockQuantity(5);p.setMainImage("/user/img/product01.png");return products.saveAndFlush(p);}
    MockMultipartHttpServletRequestBuilder form(String url,String name,String sku,boolean image){
        MockMultipartHttpServletRequestBuilder r=multipart(url);
        if(image)r.file(new MockMultipartFile("imageFile","test.png","image/png",new byte[]{(byte)137,80,78,71,13,10,26,10}));
        r.param("categoryId",category.getId().toString()).param("name",name).param("sku",sku).param("price","250").param("stockQuantity","7").param("active","true");return r;
    }
    @Test void createAssignsShopFromPrincipalAndIgnoresForgedOwnershipAndModeration() throws Exception {
        mvc.perform(form("/seller/products/new","New laptop","same-sku",true).param("shopId",bob.getId().toString()).param("isBlocked","false").with(user("alice").roles("SELLER")).with(csrf())).andExpect(status().is3xxRedirection());
        Product p=products.findById(products.findAll().stream().map(Product::getId).max(Long::compare).orElseThrow()).orElseThrow();
        assertEquals(alice.getId(),p.getShop().getId());assertEquals("SAME-SKU",p.getSku());assertEquals(new BigDecimal("250"),p.getPrice());assertEquals(7,p.getStockQuantity());
        mvc.perform(get("/seller/products").with(user("alice").roles("SELLER"))).andExpect(status().isOk()).andExpect(content().string(containsString("New laptop"))).andExpect(content().string(not(containsString("Laptop Bob"))));
    }
    @Test void skuIsUniqueWithinShopButMayMatchAnotherShop() throws Exception {
        mvc.perform(form("/seller/products/new","Same A","SHARED",true).with(user("alice").roles("SELLER")).with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(form("/seller/products/new","Same B","SHARED",true).with(user("bob").roles("SELLER")).with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(form("/seller/products/new","Duplicate","SHARED",true).with(user("alice").roles("SELLER")).with(csrf())).andExpect(status().isOk()).andExpect(content().string(containsString("Mã sản phẩm đã tồn tại trong shop")));
        assertEquals(4,products.count());verify(images,times(2)).uploadImage(any());
    }
    @Test void crossShopReadAndMutationsReturnNotFound() throws Exception {
        mvc.perform(get("/seller/products/"+b.getId()+"/edit").with(user("alice").roles("SELLER"))).andExpect(status().isNotFound());
        mvc.perform(form("/seller/products/"+b.getId()+"/edit","Stolen","BOB-1",false).with(user("alice").roles("SELLER")).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/seller/products/"+b.getId()+"/delete").with(user("alice").roles("SELLER")).with(csrf())).andExpect(status().isNotFound());
        b.setIsDeleted(true);products.flush();
        mvc.perform(post("/seller/products/"+b.getId()+"/restore").with(user("alice").roles("SELLER")).with(csrf())).andExpect(status().isNotFound());
        assertEquals("Laptop Bob",b.getName());assertTrue(b.getIsDeleted());
    }
    @Test void editKeepsShopSlugAndImageAndSoftDeleteCanRestore() throws Exception {
        String originalSlug=a.getSlug(),originalImage=a.getMainImage();
        mvc.perform(get("/seller/products/"+a.getId()+"/edit").with(user("alice").roles("SELLER"))).andExpect(status().isOk());
        mvc.perform(form("/seller/products/"+a.getId()+"/edit","Edited laptop","ALICE-EDIT",false).param("shopId",bob.getId().toString()).with(user("alice").roles("SELLER")).with(csrf())).andExpect(status().is3xxRedirection());
        assertEquals("Edited laptop",a.getName());assertEquals(originalSlug,a.getSlug());assertEquals(originalImage,a.getMainImage());assertEquals(alice.getId(),a.getShop().getId());verify(images,never()).uploadImage(any());
        mvc.perform(post("/seller/products/"+a.getId()+"/delete").with(user("alice").roles("SELLER")).with(csrf())).andExpect(status().is3xxRedirection());assertTrue(a.getIsDeleted());
        mvc.perform(get("/products/"+originalSlug)).andExpect(status().isNotFound());
        mvc.perform(get("/seller/products").param("trash","true").with(user("alice").roles("SELLER"))).andExpect(content().string(containsString("Edited laptop"))).andExpect(content().string(containsString("Khôi phục")));
        mvc.perform(post("/seller/products/"+a.getId()+"/restore").with(user("alice").roles("SELLER")).with(csrf())).andExpect(status().is3xxRedirection());assertFalse(a.getIsDeleted());
        mvc.perform(get("/products/"+originalSlug)).andExpect(status().isOk());
    }
    @Test void adminHasMonitoringButNoOperationalProductMutation() throws Exception {
        mvc.perform(get("/admin/products").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(content().string(containsString("Giám sát sản phẩm toàn sàn"))).andExpect(content().string(not(containsString("href=\"/admin/products/new\""))));
        mvc.perform(get("/admin/products/"+a.getId()).with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(content().string(containsString("Thông tin, giá và tồn kho do Seller quản lý")));
        mvc.perform(get("/admin/products/new").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/products/"+a.getId()+"/edit").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isForbidden());
    }
    @Test void moderationHidesCatalogAndCartAndSellerCannotUnblock() throws Exception {
        mvc.perform(post("/api/cart/items").with(user("buyer").roles("USER")).with(csrf()).contentType("application/json").content("{\"productId\":"+a.getId()+",\"quantity\":1}")).andExpect(status().isCreated());
        mvc.perform(post("/admin/products/"+a.getId()+"/moderation").with(user("admin").roles("ADMIN")).with(csrf()).param("blocked","true").param("expectedBlocked","false").param("reason","Thông tin chưa phù hợp")).andExpect(status().is3xxRedirection());
        assertTrue(a.getIsBlocked());assertEquals(1,history.count());
        mvc.perform(get("/products/"+a.getSlug())).andExpect(status().isNotFound());
        mvc.perform(get("/products").param("keyword","Laptop Alice")).andExpect(model().attribute("products",hasProperty("totalElements",is(0L))));
        mvc.perform(post("/api/cart/items").with(user("buyer").roles("USER")).with(csrf()).contentType("application/json").content("{\"productId\":"+a.getId()+",\"quantity\":1}")).andExpect(status().isConflict());
        mvc.perform(post("/api/orders").with(user("buyer").roles("USER")).with(csrf()).contentType("application/json").content("{\"recipientName\":\"Test\",\"recipientPhone\":\"0900000000\",\"shippingAddress\":\"Test\",\"paymentMethod\":\"COD\",\"requestKey\":\""+UUID.randomUUID()+"\"}")).andExpect(status().isConflict());
        mvc.perform(form("/seller/products/"+a.getId()+"/edit","Edited laptop","ALICE-1",false).param("isBlocked","false").param("blocked","false").with(user("alice").roles("SELLER")).with(csrf())).andExpect(status().is3xxRedirection());assertTrue(a.getIsBlocked());
        mvc.perform(post("/admin/products/"+a.getId()+"/moderation").with(user("alice").roles("SELLER")).with(csrf()).param("blocked","false").param("expectedBlocked","true").param("reason","Bypass")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/products/"+a.getId()+"/moderation").with(user("admin").roles("ADMIN")).with(csrf()).param("blocked","false").param("expectedBlocked","true").param("reason","Đã kiểm tra")).andExpect(status().is3xxRedirection());assertFalse(a.getIsBlocked());assertEquals(2,history.count());
    }
    @Test void suspendedShopIsReadOnlyAndProductsCannotBeBought() throws Exception {
        alice.setStatus(ShopStatus.SUSPENDED);shops.flush();
        mvc.perform(get("/seller/products").with(user("alice").roles("SELLER"))).andExpect(status().isOk()).andExpect(content().string(not(containsString("href=\"/seller/products/new\""))));
        mvc.perform(form("/seller/products/"+a.getId()+"/edit","Forbidden","ALICE-1",false).with(user("alice").roles("SELLER")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/seller/products/"+a.getId()+"/delete").with(user("alice").roles("SELLER")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/products/"+a.getSlug())).andExpect(status().isNotFound());
        mvc.perform(post("/api/cart/items").with(user("buyer").roles("USER")).with(csrf()).contentType("application/json").content("{\"productId\":"+a.getId()+",\"quantity\":1}")).andExpect(status().isConflict());
        assertEquals("Laptop Alice",a.getName());
    }
    @Test void userCannotReachSellerAndCsrfProtectsMutations() throws Exception {
        mvc.perform(get("/seller/products")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/seller/products").with(user("buyer").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(post("/seller/products/"+a.getId()+"/delete").with(user("alice").roles("SELLER"))).andExpect(status().isForbidden());
    }
    @Test void missingImageAndInvalidDiscountDoNotCreateProducts() throws Exception {
        mvc.perform(form("/seller/products/new","No photo","NEW",false).with(user("alice").roles("SELLER")).with(csrf())).andExpect(status().isOk()).andExpect(content().string(containsString("Vui lòng chọn ảnh chính")));
        mvc.perform(form("/seller/products/new","Bad discount","NEW",true).param("discountPrice","999").with(user("alice").roles("SELLER")).with(csrf())).andExpect(status().isOk()).andExpect(content().string(containsString("Giá khuyến mãi phải nhỏ hơn giá gốc")));
        assertEquals(2,products.count());verify(images,never()).uploadImage(any());
    }
    @Test void filtersAndPaginationRemainScopedToOwner() throws Exception {
        for(int i=0;i<11;i++)product(alice,"Extra "+i,"EXTRA-"+i);
        mvc.perform(get("/seller/products").param("page","1").with(user("alice").roles("SELLER"))).andExpect(status().isOk()).andExpect(model().attribute("products",hasProperty("totalElements",is(12L)))).andExpect(content().string(not(containsString("Laptop Bob"))));
        mvc.perform(get("/seller/products").param("keyword","BOB-1").with(user("alice").roles("SELLER"))).andExpect(model().attribute("products",hasProperty("totalElements",is(0L))));
        mvc.perform(get("/admin/products").param("shopId",bob.getId().toString()).with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(model().attribute("products",hasProperty("totalElements",is(1L)))).andExpect(content().string(containsString("Laptop Bob")));
    }
}
