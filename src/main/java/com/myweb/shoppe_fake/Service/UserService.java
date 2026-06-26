package com.myweb.shoppe_fake.Service;

import com.myweb.shoppe_fake.Model.User;
import com.myweb.shoppe_fake.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService implements UserDetailsService{
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

    public Page<User> getPaginatedUsers(int pageNo, int pageSize, String keyword) {
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize);
        if (keyword != null && !keyword.isEmpty()) {
            return userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword, pageable);
        }
        return userRepository.findAll(pageable);
    }

    public User saveUser(User user) {
        if(user.getId() == null && userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại!");
        }
        return userRepository.save(user);
    }
    public User getUserById(String id) {
        Optional<User> optional = userRepository.findById(id);
        return optional.orElse(null);
    }
    // Chức năng Khóa / Mở khóa
    public void toggleUserStatus(String id) {
        User user = getUserById(id);
        if (user != null) {
            user.setActive(!user.isActive());
            userRepository.save(user);
        }
    }
    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }
    public String registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            return "Tên đăng nhập đã tồn tại!";
        }
        // Cần thêm hàm existsByEmail vào UserRepository của bạn
        if (userRepository.existsByEmail(user.getEmail())) {
               return "Email đã được sử dụng!";
        }

        user.setPassword(passwordEncoder.encode(user.getPassword())); // Mã hóa mật khẩu
        user.setActive(true); // Mặc định mở khóa khi người dùng tự đăng ký
        userRepository.save(user);
        return "success";
    }

    public String generateResetToken(String email) {
        // Cần thêm hàm findByEmail vào UserRepository của bạn
        User user = userRepository.findByEmail(email);
        if (user == null) return "Không tìm thấy tài khoản với email này!";

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        userRepository.save(user);

        // Chuẩn bị và gửi email
        String resetLink = "http://localhost:8080/reset-password?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Yêu cầu đặt lại mật khẩu - Shoppe Fake");
        message.setText("Click vào link sau để đặt lại mật khẩu: " + resetLink);
        mailSender.send(message);

        return "success";
    }

    public void updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null); // Xóa token sau khi cập nhật thành công
        userRepository.save(user);
    }

    // Hàm bắt buộc của interface UserDetailsService để Spring Security xử lý Đăng Nhập
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("===> Đang kiểm tra đăng nhập cho tài khoản: " + username);
        // Cần thêm hàm findByUsername vào UserRepository của bạn
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username));
        System.out.println("===> Mật khẩu lấy từ DB: " + user.getPassword());
        String[] userAuthorities = user.getRoles() != null 
                                ? user.getRoles().toArray(new String[0]) 
                                : new String[]{"ROLE_USER"}; // Mặc định nếu null

    return org.springframework.security.core.userdetails.User
            .withUsername(user.getUsername())
            .password(user.getPassword())
            .disabled(!user.isActive())
            // DÙNG authorities THAY VÌ roles ĐỂ TRÁNH LỖI GẤP ĐÔI CHỮ "ROLE_"
            .authorities(userAuthorities) 
            .build();
    }
    public void resetPassword(String token, String newPassword) throws Exception {
        // 1. Tìm User có chứa token này trong Database
        User user = userRepository.findByResetToken(token);

        // 2. Nếu không tìm thấy (token sai hoặc đã bị xóa) -> Báo lỗi
        if (user == null) {
            throw new Exception("Đường dẫn đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);

        // 4. Xóa token đi để link này không thể dùng lại được nữa
        user.setResetToken(null); 
        userRepository.save(user);
    }
    
}