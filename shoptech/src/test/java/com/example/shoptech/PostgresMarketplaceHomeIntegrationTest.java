package com.example.shoptech;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in only. Reuses the commerce tests against the repository's PostgreSQL schema. */
@EnabledIfEnvironmentVariable(named="PERSON3_POSTGRES_TEST_URL",matches=".+")
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
class PostgresMarketplaceHomeIntegrationTest extends MarketplaceHomeIntegrationTest {
    private static final String SCHEMA="person3_"+UUID.randomUUID().toString().replace("-","");
    private static boolean created;

    static void validateTarget(String url) {
        if(url==null || !url.startsWith("jdbc:postgresql://"))throw new IllegalArgumentException("A dedicated local PostgreSQL test database is required.");
        URI target=URI.create(url.substring(5));
        if(!java.util.Set.of("127.0.0.1","localhost","[::1]").contains(target.getHost())
                || !"/shoptech_person3_test".equals(target.getPath()) || target.getUserInfo()!=null || target.getRawQuery()!=null || target.getFragment()!=null)
            throw new IllegalArgumentException("Only a loopback shoptech_person3_test database without URL credentials/options is allowed.");
    }
    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(System.getenv("PERSON3_POSTGRES_TEST_URL"),
                System.getenv("PERSON3_POSTGRES_TEST_USER"),System.getenv("PERSON3_POSTGRES_TEST_PASSWORD"));
    }
    @DynamicPropertySource static void postgres(DynamicPropertyRegistry properties) throws Exception {
        String url=System.getenv("PERSON3_POSTGRES_TEST_URL");validateTarget(url);
        if(System.getenv("PERSON3_POSTGRES_TEST_USER")==null || System.getenv("PERSON3_POSTGRES_TEST_PASSWORD")==null)
            throw new IllegalArgumentException("Set the dedicated PostgreSQL test user and password environment variables.");
        try(Connection connection=connection();Statement statement=connection.createStatement()) {
            statement.execute("CREATE SCHEMA "+SCHEMA);created=true;
            statement.execute("SET search_path TO "+SCHEMA);
            String source=new ClassPathResource("db/schema.sql").getContentAsString(StandardCharsets.UTF_8);
            // The shared schema file starts with destructive cleanup and an optional extension.
            // A fresh private schema needs neither; never execute those statements.
            source=source.replaceAll("(?im)^\\s*(?:DROP (?:TABLE|VIEW)|CREATE EXTENSION)[^;]*;","");
            statement.execute(source);
            // Add the marketplace tables in this private, empty test schema only.
            // Never execute the sandbox migration's bootstrap/backfill or database guard here.
            String foundation=new ClassPathResource("db/migrations/V001__marketplace_foundation.sql").getContentAsString(StandardCharsets.UTF_8);
            statement.execute(foundation.substring(foundation.indexOf("CREATE TABLE shops ("),foundation.indexOf("-- No business commission")));
            String ownership=new ClassPathResource("db/migrations/V002__shop_owned_products.sql").getContentAsString(StandardCharsets.UTF_8);
            statement.execute(ownership.substring(ownership.indexOf("ALTER TABLE products ADD COLUMN shop_id"),ownership.indexOf("INSERT INTO marketplace_migrations")));
            String splits=new ClassPathResource("db/migrations/V003__shop_orders.sql").getContentAsString(StandardCharsets.UTF_8);
            statement.execute(splits.substring(splits.indexOf("CREATE TABLE shop_orders ("),splits.indexOf("-- All historical")));
            statement.execute("ALTER TABLE order_details ALTER COLUMN shop_order_id SET NOT NULL");
            String community=new ClassPathResource("db/migrations/V004__reviews_and_favorites.sql").getContentAsString(StandardCharsets.UTF_8);
            statement.execute(community.substring(community.indexOf("-- DDL START"),community.indexOf("-- DDL END")));
        }
        properties.add("spring.datasource.url",()->url);
        properties.add("spring.datasource.driver-class-name",()->"org.postgresql.Driver");
        properties.add("spring.datasource.username",()->System.getenv("PERSON3_POSTGRES_TEST_USER"));
        properties.add("spring.datasource.password",()->System.getenv("PERSON3_POSTGRES_TEST_PASSWORD"));
        properties.add("spring.datasource.hikari.schema",()->SCHEMA);
        properties.add("spring.jpa.hibernate.ddl-auto",()->"validate");
        properties.add("spring.sql.init.mode",()->"never");
    }
    @AfterAll static void cleanup() throws SQLException {
        if(!created)return;
        validateTarget(System.getenv("PERSON3_POSTGRES_TEST_URL"));
        if(!SCHEMA.matches("person3_[0-9a-f]{32}"))throw new IllegalStateException("Invalid test schema name.");
        try(Connection connection=connection();Statement statement=connection.createStatement()) {
            statement.execute("DROP SCHEMA "+SCHEMA+" CASCADE");
        }
    }
}
