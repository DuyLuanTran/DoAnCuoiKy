package com.example.shoptech;

import com.example.shoptech.entity.Role;
import com.example.shoptech.repository.RoleRepository;
import com.example.shoptech.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.config.location=classpath:/application-test.properties",
        "spring.datasource.url=jdbc:h2:mem:auth-register-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@Transactional
class AuthRegisterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        if (roleRepository.findByName("ROLE_USER").isEmpty()) {
            roleRepository.save(new Role(null, "ROLE_USER", "Khách hàng"));
        }
    }

    @Test
    void getRegisterPage_shouldReturn200AndRenderRegisterView() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/register"))
                .andExpect(model().attributeExists("registerForm"))
                .andExpect(content().string(containsString("Tạo tài khoản")))
                .andExpect(content().string(containsString("fullName")))
                .andExpect(content().string(containsString("username")))
                .andExpect(content().string(containsString("email")))
                .andExpect(content().string(containsString("phoneNumber")))
                .andExpect(content().string(containsString("address")))
                .andExpect(content().string(containsString("password")))
                .andExpect(content().string(containsString("/login")));
    }

    @Test
    void postRegister_withInvalidData_shouldReturn200AndRenderErrorsWithout500() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "ab") // too short, min is 3
                        .param("email", "not-an-email")
                        .param("password", "123") // too short, min is 6
                        .param("fullName", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/register"))
                .andExpect(model().hasErrors());
    }

    @Test
    void postRegister_withValidData_shouldSaveUserAndRedirectToLogin() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Nguyễn Văn Test")
                        .param("username", "testuser_reg")
                        .param("email", "testreg@example.com")
                        .param("phoneNumber", "0912345678")
                        .param("address", "123 Đường Test, TP.HCM")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        assertTrue(userRepository.existsByUsername("testuser_reg"));
        assertTrue(userRepository.existsByEmail("testreg@example.com"));
    }

    @Test
    void unauthenticatedShopRegistration_shouldRedirectToLoginWithParam() throws Exception {
        mockMvc.perform(get("/shop-registration"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?redirect=/shop-registration"));
    }

    @Test
    void getLoginPage_withShopRegistrationRedirect_shouldShowNotice() throws Exception {
        mockMvc.perform(get("/login").param("redirect", "/shop-registration"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/login"))
                .andExpect(content().string(containsString("Cần đăng nhập tài khoản")))
                .andExpect(content().string(containsString("/shop-registration")));
    }

    @Test
    void getRegisterPage_withShopRegistrationRedirect_shouldShowNotice() throws Exception {
        mockMvc.perform(get("/register").param("redirect", "/shop-registration"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/register"))
                .andExpect(content().string(containsString("Đăng ký tài khoản người bán")))
                .andExpect(content().string(containsString("/shop-registration")));
    }
}
