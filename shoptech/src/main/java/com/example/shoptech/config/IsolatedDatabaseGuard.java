package com.example.shoptech.config;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
@Component @Profile("isolated")
public class IsolatedDatabaseGuard implements InitializingBean {
    private final Environment environment; private final DataSource dataSource;
    public IsolatedDatabaseGuard(Environment environment,DataSource dataSource){this.environment=environment;this.dataSource=dataSource;}
    @Override public void afterPropertiesSet() throws Exception {
        String expected="jdbc:postgresql://127.0.0.1:55432/shoptech_marketplace";
        if(!expected.equals(environment.getProperty("spring.datasource.url")))throw new IllegalStateException("Sandbox refused a non-isolated datasource URL.");
        if(!"none".equals(environment.getProperty("spring.jpa.hibernate.ddl-auto")))throw new IllegalStateException("Sandbox requires explicit SQL migrations.");
        if(!"isolated-sandbox".equals(environment.getProperty("cloudinary.cloud-name")))throw new IllegalStateException("Sandbox must not use real Cloudinary credentials.");
        if(!"127.0.0.1".equals(environment.getProperty("spring.mail.host"))||!"1".equals(environment.getProperty("spring.mail.port")))throw new IllegalStateException("Sandbox outbound email must be disabled.");
        if(!environment.getProperty("gemini.api.key","").isBlank())throw new IllegalStateException("Sandbox chatbot requests must be disabled.");
        try(var c=dataSource.getConnection();var s=c.createStatement();var r=s.executeQuery("select current_database()")){
            if(!r.next()||!"shoptech_marketplace".equals(r.getString(1)))throw new IllegalStateException("Unexpected sandbox database.");
        }
    }
}
