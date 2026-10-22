package com.example.shoptech;

import com.example.shoptech.entity.*;
import com.example.shoptech.repository.*;
import org.springframework.boot.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;
import java.util.Set;

/** Local browser fixture. It is test-only and always uses an ephemeral H2 database. */
public class Person3TestApplication {
    public static void main(String[] args) {
        if (System.getenv("PERSON3_TEST_PASSWORD")==null || System.getenv("PERSON3_TEST_PASSWORD").length()<12)
            throw new IllegalStateException("Set PERSON3_TEST_PASSWORD to a temporary test password of at least 12 characters.");
        System.setProperty("spring.config.location","classpath:/application-test.properties");
        System.setProperty("spring.datasource.url","jdbc:h2:mem:person3-browser;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        System.setProperty("spring.datasource.driver-class-name","org.h2.Driver");
        System.setProperty("spring.datasource.username","sa");
        System.setProperty("spring.datasource.password","");
        System.setProperty("spring.jpa.hibernate.ddl-auto","create-drop");
        System.setProperty("server.port","18080");
        System.setProperty("server.address","127.0.0.1");
        System.setProperty("shoptech.public-base-url","http://localhost:18080");
        SpringApplication.run(new Class<?>[]{ShoptechApplication.class,Fixtures.class},new String[0]);
    }
    @TestConfiguration
    static class Fixtures {
        @Bean org.springframework.web.servlet.function.RouterFunction<org.springframework.web.servlet.function.ServerResponse> person3QaSnapshot(
                org.springframework.jdbc.core.JdbcTemplate jdbc, javax.sql.DataSource source) {
            return org.springframework.web.servlet.function.RouterFunctions.route()
                    .GET("/admin/person3-test/snapshot", request -> {
                        String url;
                        try (var connection=source.getConnection()) {
                            url=connection.getMetaData().getURL();
                            if(!"H2".equals(connection.getMetaData().getDatabaseProductName()) || !url.startsWith("jdbc:h2:mem:person3-browser"))
                                throw new IllegalStateException("QA snapshot is restricted to the isolated browser H2 database.");
                        }
                        return org.springframework.web.servlet.function.ServerResponse.ok().body(java.util.Map.of(
                                "datasource",url,"database","H2",
                                "products",jdbc.queryForList("select id,name,price,discount_price,stock_quantity from products order by id"),
                                "cartItems",jdbc.queryForList("select ci.id,c.user_id,ci.product_id,ci.quantity from cart_items ci join carts c on c.id=ci.cart_id order by ci.id"),
                                "orders",jdbc.queryForList("select id,user_id,order_code,subtotal_amount,shipping_fee,total_amount,payment_method,payment_status,order_status,is_deleted,created_at from orders order by id"),
                                "details",jdbc.queryForList("select order_id,product_id,product_name,price,quantity,subtotal from order_details order by id"),
                                "revenue",jdbc.queryForList("select coalesce(sum(total_amount),0) as revenue,count(*) as completed_orders from orders where order_status='COMPLETED' and is_deleted=false")));
                    }).build();
        }
        @Bean CommandLineRunner browserData(UserRepository users,RoleRepository roles,CategoryRepository categories,ProductRepository products,PasswordEncoder encoder) {
            return args->{
                Role customer=roles.save(new Role(null,"ROLE_USER","Test"));
                Role admin=roles.save(new Role(null,"ROLE_ADMIN","Test"));
                String password=encoder.encode(System.getenv("PERSON3_TEST_PASSWORD"));
                users.save(User.builder().username("person3-user").email("buyer@example.invalid").fullName("Khách hàng thử nghiệm").password(password).roles(Set.of(customer)).build());
                users.save(User.builder().username("person3-admin").email("admin@example.invalid").fullName("Quản trị thử nghiệm").password(password).roles(Set.of(admin)).build());
                users.save(User.builder().username("person3-other").email("other@example.invalid").fullName("Khách hàng đối chứng").password(password).roles(Set.of(customer)).build());
                Category category=new Category();category.setName("Thiết bị thử nghiệm");category.setSlug("test-devices");categories.save(category);
                Product product=new Product();product.setName("Laptop thử nghiệm");product.setSlug("test-laptop");product.setCategory(category);product.setPrice(new BigDecimal("15000000"));product.setDiscountPrice(new BigDecimal("14000000"));product.setStockQuantity(5);products.save(product);
            };
        }
    }
}
