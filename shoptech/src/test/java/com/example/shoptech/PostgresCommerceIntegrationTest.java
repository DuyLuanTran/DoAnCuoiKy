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
class PostgresCommerceIntegrationTest extends CommerceIntegrationTest {
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
    @Test void usesRealPostgresSchemaConstraintsAndArrayMapping() {
        assertEquals("PostgreSQL",jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<String>)c->c.getMetaData().getDatabaseProductName()));
        assertEquals(SCHEMA,jdbc.queryForObject("SELECT current_schema()",String.class));
        assertEquals("validate",entityManager.getEntityManagerFactory().getProperties().get("hibernate.hbm2ddl.auto"));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM pg_constraint c JOIN pg_namespace n ON n.oid=c.connamespace WHERE n.nspname=? AND c.conname='uq_cart_product'",Integer.class,SCHEMA));
        new org.springframework.transaction.support.TransactionTemplate(manager).executeWithoutResult(s->products.findById(productId).orElseThrow().setImageGallery(new String[]{"https://example.invalid/test.png"}));
        assertArrayEquals(new String[]{"https://example.invalid/test.png"},products.findById(productId).orElseThrow().getImageGallery());
    }
}
