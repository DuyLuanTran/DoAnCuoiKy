package com.example.shoptech;
import com.example.shoptech.entity.*;
import com.example.shoptech.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(properties={"spring.config.location=classpath:/application-test.properties","spring.datasource.url=jdbc:h2:mem:marketplace-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc @Transactional
class MarketplaceShopIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired ShopRepository shops;
    @Autowired MarketplaceSettingsRepository settings;
    @Autowired ShopReviewHistoryRepository history;
    @BeforeEach void seed(){
        Role buyer=role("ROLE_USER"),admin=role("ROLE_ADMIN");role("ROLE_SELLER");
        account("alice",buyer);account("bob",buyer);account("admin",admin);
        MarketplaceSettings s=new MarketplaceSettings();s.setDefaultCommissionRate(new BigDecimal("3.00"));settings.saveAndFlush(s);
    }
    Role role(String name){return roles.findByName(name).orElseGet(()->roles.save(new Role(null,name,"test")));}
    void account(String name,Role role){users.saveAndFlush(User.builder().username(name).email(name+"@example.invalid").password("unused").fullName(name).roles(new HashSet<>(Set.of(role))).build());}
    void register(String username,String name) throws Exception {
        mvc.perform(post("/shop-registration").with(user(username).roles("USER")).with(csrf()).param("name",name).param("email",username+"@example.invalid").param("phone","0900000000").param("address","Địa chỉ thử nghiệm").param("description","Shop thử nghiệm"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/shop-registration"));
    }
    void review(Long id,String action,String expected) throws Exception {
        mvc.perform(post("/admin/shops/"+id+"/"+action).with(user("admin").roles("ADMIN")).with(csrf()).param("expectedStatus",expected).param("reason","Kiểm tra hồ sơ"))
            .andExpect(status().is3xxRedirection());
    }
    @Test void registrationStaysPendingUntilAdminApproves() throws Exception {
        register("alice","Công nghệ An Khang");Shop s=shops.findByOwnerUsername("alice").orElseThrow();
        assertEquals(ShopStatus.PENDING,s.getStatus());assertFalse(users.findByUsernameAndIsDeletedFalse("alice").orElseThrow().getRoles().stream().anyMatch(r->r.getName().equals("ROLE_SELLER")));
        mvc.perform(get("/seller").with(user("alice").roles("USER"))).andExpect(status().isForbidden());
        review(s.getId(),"approve","PENDING");assertEquals(ShopStatus.ACTIVE,shops.findById(s.getId()).orElseThrow().getStatus());
        assertTrue(users.findByUsernameAndIsDeletedFalse("alice").orElseThrow().getRoles().stream().anyMatch(r->r.getName().equals("ROLE_SELLER")));
        mvc.perform(get("/seller").with(user("alice").roles("USER","SELLER"))).andExpect(status().isOk()).andExpect(content().string(containsString("Công nghệ An Khang"))).andExpect(content().string(not(containsString("href=\"/admin/users\""))));
        mvc.perform(get("/admin/shops").with(user("alice").roles("SELLER"))).andExpect(status().isForbidden());
    }
    @Test void pagesRenderAndAdminCanFilterPendingShops() throws Exception {
        mvc.perform(get("/shop-registration").with(user("alice").roles("USER"))).andExpect(status().isOk()).andExpect(content().string(containsString("Đăng ký mở shop")));
        register("alice","Shop Alpha");
        mvc.perform(get("/shop-registration").with(user("alice").roles("USER"))).andExpect(status().isOk()).andExpect(content().string(containsString("PENDING")));
        mvc.perform(get("/admin/shops").param("status","PENDING").param("keyword","Alpha").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(content().string(containsString("Shop Alpha")));
        mvc.perform(get("/admin/shops/"+shops.findByOwnerUsername("alice").orElseThrow().getId()).with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(content().string(containsString("Duyệt đăng ký shop")));
    }
    @Test void duplicateRegistrationAndInvalidInputDoNotCreateExtraShops() throws Exception {
        register("alice","Shop Alpha");
        mvc.perform(post("/shop-registration").with(user("alice").roles("USER")).with(csrf()).param("name","Duplicate").param("email","alice@example.invalid").param("phone","0900000000").param("address","A"))
            .andExpect(status().isOk());assertEquals(1,shops.count());
        mvc.perform(post("/shop-registration").with(user("bob").roles("USER")).with(csrf()).param("name","").param("email","bad").param("phone","bad").param("address",""))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Email không hợp lệ")));assertEquals(1,shops.count());
    }
    @Test void rejectedOwnerCanResubmitAndStaleApprovalIsRejected() throws Exception {
        register("alice","Shop Alpha");Long id=shops.findByOwnerUsername("alice").orElseThrow().getId();review(id,"reject","PENDING");assertEquals(ShopStatus.REJECTED,shops.findById(id).orElseThrow().getStatus());
        mvc.perform(post("/admin/shops/"+id+"/approve").with(user("admin").roles("ADMIN")).with(csrf()).param("expectedStatus","PENDING")).andExpect(flash().attributeExists("error"));
        register("alice","Shop Alpha mới");assertEquals(1,shops.count());assertEquals(ShopStatus.PENDING,shops.findById(id).orElseThrow().getStatus());assertEquals(2,history.count());
    }
    @Test void sellerCannotEditAnotherShopOrAssignOwnCommission() throws Exception {
        register("alice","Shop Alice");register("bob","Shop Bob");Long a=shops.findByOwnerUsername("alice").orElseThrow().getId(),b=shops.findByOwnerUsername("bob").orElseThrow().getId();review(a,"approve","PENDING");review(b,"approve","PENDING");
        mvc.perform(post("/seller/shop").with(user("alice").roles("SELLER")).with(csrf()).param("id",b.toString()).param("shopId",b.toString()).param("commissionRate","0").param("name","Alice updated").param("email","alice@example.invalid").param("phone","0900000000").param("address","New address"))
            .andExpect(status().is3xxRedirection());assertEquals("Shop Bob",shops.findById(b).orElseThrow().getName());assertEquals("Alice updated",shops.findById(a).orElseThrow().getName());assertNull(shops.findById(a).orElseThrow().getCommissionRate());
        mvc.perform(post("/admin/shops/"+b+"/commission").with(user("alice").roles("SELLER")).with(csrf()).param("rate","0")).andExpect(status().isForbidden());
    }
    @Test void suspensionBlocksChangesAndAdminCanResume() throws Exception {
        register("alice","Shop Alpha");Long id=shops.findByOwnerUsername("alice").orElseThrow().getId();review(id,"approve","PENDING");review(id,"suspend","ACTIVE");
        mvc.perform(get("/seller/shop").with(user("alice").roles("SELLER"))).andExpect(status().isOk()).andExpect(content().string(containsString("disabled")));
        mvc.perform(post("/seller/shop").with(user("alice").roles("SELLER")).with(csrf()).param("name","Forbidden rename").param("email","alice@example.invalid").param("phone","0900000000").param("address","Test"))
            .andExpect(status().isOk());assertEquals("Shop Alpha",shops.findById(id).orElseThrow().getName());
        review(id,"resume","SUSPENDED");assertEquals(ShopStatus.ACTIVE,shops.findById(id).orElseThrow().getStatus());
    }
    @Test void commissionsRequireAdminAndRejectOutOfRange() throws Exception {
        mvc.perform(post("/admin/shops/commission").with(user("admin").roles("ADMIN")).with(csrf()).param("rate","7.25")).andExpect(flash().attributeExists("success"));assertEquals(0,new BigDecimal("7.25").compareTo(settings.findById(1).orElseThrow().getDefaultCommissionRate()));
        mvc.perform(post("/admin/shops/commission").with(user("admin").roles("ADMIN")).with(csrf()).param("rate","101")).andExpect(flash().attributeExists("error"));assertEquals(0,new BigDecimal("7.25").compareTo(settings.findById(1).orElseThrow().getDefaultCommissionRate()));
        register("alice","Shop Alpha");Long id=shops.findByOwnerUsername("alice").orElseThrow().getId();
        mvc.perform(post("/admin/shops/"+id+"/commission").with(user("admin").roles("ADMIN")).with(csrf()).param("rate","5.50")).andExpect(flash().attributeExists("success"));assertEquals(0,new BigDecimal("5.50").compareTo(shops.findById(id).orElseThrow().getCommissionRate()));
        mvc.perform(post("/admin/shops/"+id+"/commission").with(user("admin").roles("ADMIN")).with(csrf()).param("rate","")).andExpect(flash().attributeExists("success"));assertNull(shops.findById(id).orElseThrow().getCommissionRate());
    }
    @Test void csrfAndLoginRequired() throws Exception {
        mvc.perform(get("/shop-registration")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/shop-registration").with(user("alice").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/shops/commission").with(user("admin").roles("ADMIN")).param("rate","5")).andExpect(status().isForbidden());
    }
}
