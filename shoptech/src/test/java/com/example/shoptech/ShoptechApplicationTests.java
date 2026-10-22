package com.example.shoptech;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.config.location=classpath:/application-test.properties")
class ShoptechApplicationTests {
    @org.springframework.beans.factory.annotation.Autowired
    org.springframework.context.ApplicationContext applicationContext;
    @Test void demoSnapshotToolIsAbsentFromNormalApplication() {
        org.junit.jupiter.api.Assertions.assertFalse(applicationContext.containsBean("person3QaSnapshot"));
    }

	@Test
	void contextLoads() {
	}

    @Test void defaultConfigurationCannotMutateSharedSchema() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withPropertyValues("spring.config.import=")
                .withInitializer(new org.springframework.boot.test.context.ConfigDataApplicationContextInitializer())
                .run(context -> {
                    org.junit.jupiter.api.Assertions.assertEquals("none",context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto"));
                    org.junit.jupiter.api.Assertions.assertEquals("never",context.getEnvironment().getProperty("spring.sql.init.mode"));
                    org.junit.jupiter.api.Assertions.assertFalse(context.containsBean("dataSource"));
                });
    }
    @Test void postgresSuiteRejectsSharedOrAmbiguousDatabaseTargets() {
        for(String url:java.util.List.of("jdbc:postgresql://shared.supabase.com:5432/postgres",
                "jdbc:postgresql://localhost:5432/postgres","jdbc:postgresql://localhost:5432/shoptech_person3_test?currentSchema=public",
                "jdbc:postgresql://user@localhost:5432/shoptech_person3_test"))
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,()->PostgresCommerceIntegrationTest.validateTarget(url));
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(()->PostgresCommerceIntegrationTest.validateTarget("jdbc:postgresql://127.0.0.1:5432/shoptech_person3_test"));
    }

}
