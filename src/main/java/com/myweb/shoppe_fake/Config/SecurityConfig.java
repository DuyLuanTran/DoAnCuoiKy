package com.myweb.shoppe_fake.Config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.myweb.shoppe_fake.Service.UserService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserService userService;

   
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .userDetailsService(userService) // ← thêm dòng này
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/login", "/register", "/forgot-password", 
                                 "/reset-password", "/", "/error").permitAll()
                .requestMatchers("/user/**", "/admin/lib/**", "/css/**", 
                                 "/js/**", "/img/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler((request, response, authentication) -> {
        // Lấy danh sách quyền (authorities) của user vừa đăng nhập thành công
            var authorities = authentication.getAuthorities();
        
        // Kiểm tra xem user này có quyền ROLE_ADMIN hay không
           boolean isAdmin = authorities.stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
            if (isAdmin) {
            // Nếu là Admin, đưa thẳng vào trang admin
            response.sendRedirect("/admin"); 
           } else {
            // Nếu là người dùng thường, đưa về trang chủ
                 response.sendRedirect("/");
            }
    })
                .permitAll()
            )
            .logout(logout -> logout
                  .logoutUrl("/logout")
                  .logoutSuccessUrl("/")
                  .invalidateHttpSession(true)   // Hủy session hiện tại
                  .clearAuthentication(true)     // Xóa thông tin xác thực khỏi SecurityContext
                  .deleteCookies("JSESSIONID")   // Xóa cookie chứa Session ID trên trình duyệt
                  .permitAll()
)
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}