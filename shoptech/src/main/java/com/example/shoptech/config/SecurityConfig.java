package com.example.shoptech.config;

import com.example.shoptech.security.JwtCookieAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public FilterRegistrationBean<JwtCookieAuthenticationFilter> jwtFilterRegistration(
            JwtCookieAuthenticationFilter jwtFilter
    ) {
        FilterRegistrationBean<JwtCookieAuthenticationFilter> registration =
                new FilterRegistrationBean<>(jwtFilter);

        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtCookieAuthenticationFilter jwtFilter,
            @Value("${shoptech.security.cookie-secure:false}") boolean secureCookie
    ) throws Exception {

        CookieCsrfTokenRepository csrfTokenRepository =
                CookieCsrfTokenRepository.withHttpOnlyFalse();

        csrfTokenRepository.setCookiePath("/");
        csrfTokenRepository.setCookieCustomizer(cookie ->
                cookie.secure(secureCookie).sameSite("Lax")
        );

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        // JWT xác thực lại mỗi request:
                        // không xóa CSRF token sau mỗi lần xác thực lại.
                        .sessionAuthenticationStrategy(
                                new NullAuthenticatedSessionStrategy()
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/admin/css/**",
                                "/admin/js/**",
                                "/admin/img/**",
                                "/admin/vendor/**",
                                "/user/css/**",
                                "/user/js/**",
                                "/user/img/**",
                                "/user/fonts/**"
                        ).permitAll()
                        .requestMatchers(
                                "/login",
                                "/register",
                                "/forgot-password",
                                "/reset-password"
                        ).permitAll()
                        .requestMatchers("/api/cart/**", "/api/cart", "/cart", "/checkout", "/orders/**", "/api/orders/**", "/ws/**").authenticated()
                        .requestMatchers("/account", "/favorites", "/api/favorites/**", "/api/reviews/**").authenticated()
                        .requestMatchers("/shop-registration", "/shop-registration/**").authenticated()
                        .requestMatchers("/seller/**", "/seller").hasRole("SELLER")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/user/**").authenticated()
                        .anyRequest().permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .requestCache(requestCache -> requestCache.disable())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                (request, response, failure) -> {
                                    if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
                                        response.setStatus(401);
                                        response.setContentType("application/problem+json;charset=UTF-8");
                                        response.getWriter().write("{\"status\":401,\"detail\":\"Vui lòng đăng nhập.\"}");
                                    } else {
                                        String uri = request.getRequestURI();
                                        String contextPath = request.getContextPath();
                                        String path = uri.substring(contextPath.length());
                                        if (path.startsWith("/shop-registration")) {
                                            response.sendRedirect(contextPath + "/login?redirect=/shop-registration");
                                        } else {
                                            new LoginUrlAuthenticationEntryPoint("/login").commence(request, response, failure);
                                        }
                                    }
                                }
                        )
                        .accessDeniedHandler((request, response, failure) -> {
                            if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
                                response.setStatus(403);
                                response.setContentType("application/problem+json;charset=UTF-8");
                                response.getWriter().write(
                                        "{\"status\":403,\"detail\":\"Yêu cầu API bị từ chối. Hãy tải lại trang để làm mới token bảo mật rồi thử lại.\"}"
                                );
                            } else {
                                response.sendError(403);
                            }
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .addLogoutHandler((request, response, authentication) ->
                                JwtCookieAuthenticationFilter.clearCookie(
                                        response,
                                        secureCookie
                                )
                        )
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.sendRedirect("/login?logout")
                        )
                        .permitAll()
                )
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
