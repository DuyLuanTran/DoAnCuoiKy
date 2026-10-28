package com.example.shoptech;
import com.example.shoptech.config.IsolatedDatabaseGuard;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class IsolatedDatabaseGuardTest {
    MockEnvironment safe(){return new MockEnvironment().withProperty("spring.datasource.url","jdbc:postgresql://127.0.0.1:55432/shoptech_marketplace").withProperty("spring.jpa.hibernate.ddl-auto","none").withProperty("cloudinary.cloud-name","isolated-sandbox").withProperty("spring.mail.host","127.0.0.1").withProperty("spring.mail.port","1").withProperty("gemini.api.key","");}
    @Test void rejectsNonSandboxDatabaseBeforeConnecting(){DataSource ds=mock(DataSource.class);MockEnvironment env=safe().withProperty("spring.datasource.url","jdbc:postgresql://example.invalid:5432/postgres");assertThrows(IllegalStateException.class,()->new IsolatedDatabaseGuard(env,ds).afterPropertiesSet());verifyNoInteractions(ds);}
    @Test void rejectsRealExternalServicesBeforeConnecting(){for(String[] setting:new String[][]{{"cloudinary.cloud-name","real-cloud"},{"spring.mail.host","smtp.example.invalid"},{"gemini.api.key","not-a-real-key"},{"spring.jpa.hibernate.ddl-auto","update"}}){DataSource ds=mock(DataSource.class);MockEnvironment env=safe().withProperty(setting[0],setting[1]);assertThrows(IllegalStateException.class,()->new IsolatedDatabaseGuard(env,ds).afterPropertiesSet());verifyNoInteractions(ds);}}
}
