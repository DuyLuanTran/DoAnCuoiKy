package com.example.shoptech.config;
import com.example.shoptech.entity.*;
import com.example.shoptech.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
@Configuration @Profile("isolated") @DependsOn("isolatedDatabaseGuard")
public class IsolatedDemoAccounts {
    @Bean CommandLineRunner sandboxAccounts(UserRepository users,RoleRepository roles,PasswordEncoder encoder,@Value("${SANDBOX_DEMO_PASSWORD}") String password){
        return args->{
            if(password.length()<12)throw new IllegalStateException("A temporary demo password of at least 12 characters is required.");
            create(users,roles,encoder,"sandbox-admin","Quản trị sàn thử nghiệm","ROLE_ADMIN",password);
            create(users,roles,encoder,"sandbox-user","Người bán thử nghiệm","ROLE_USER",password);
            create(users,roles,encoder,"sandbox-other","Khách hàng đối chứng","ROLE_USER",password);
        };
    }
    private void create(UserRepository users,RoleRepository roles,PasswordEncoder encoder,String username,String name,String role,String password){
        if(users.existsByUsername(username))return;
        users.save(User.builder().username(username).email(username+"@example.invalid").fullName(name).password(encoder.encode(password)).phoneNumber("0900000000").address("Địa chỉ thử nghiệm").roles(Set.of(roles.findByName(role).orElseThrow())).build());
    }
}
