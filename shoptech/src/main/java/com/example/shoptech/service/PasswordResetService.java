package com.example.shoptech.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import com.example.shoptech.entity.PasswordResetToken;
import com.example.shoptech.entity.User;
import com.example.shoptech.repository.PasswordResetTokenRepository;
import com.example.shoptech.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;
    private static final int TOKEN_VALIDITY_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final String publicBaseUrl;
    private final String fromAddress;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            JavaMailSender mailSender,
            @Value("${shoptech.public-base-url}") String publicBaseUrl,
            @Value("${shoptech.mail.from:no-reply@shoptech.local}") String fromAddress) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
        this.fromAddress = fromAddress;
    }

    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email.trim())
                .filter(existingUser -> Boolean.TRUE.equals(existingUser.getIsActive()))
                .orElse(null);
        if (user == null) {
            return;
        }

        String rawToken = createRawToken();
        String hashedToken = hashToken(rawToken);
        tokenRepository.invalidateUnusedTokensForUser(user.getId());

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(hashedToken);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(TOKEN_VALIDITY_MINUTES));
        resetToken.setIsUsed(false);
        tokenRepository.save(resetToken);

        String resetLink = UriComponentsBuilder.fromUriString(publicBaseUrl)
                .path("/reset-password")
                .queryParam("token", rawToken)
                .build()
                .encode()
                .toUriString();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(user.getEmail());
        message.setSubject("Đặt lại mật khẩu ShopTech");
        message.setText("Xin chào " + user.getFullName() + ",\n\n"
                + "Bạn vừa yêu cầu đặt lại mật khẩu. Mở liên kết dưới đây trong vòng "
                + TOKEN_VALIDITY_MINUTES + " phút:\n\n"
                + resetLink + "\n\n"
                + "Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email này.");
        mailSender.send(message);
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
        }
        if (newPassword == null || newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu không được vượt quá giới hạn hỗ trợ.");
        }

        PasswordResetToken token = tokenRepository.findByTokenAndIsUsedFalse(hashToken(rawToken))
                .filter(resetToken -> resetToken.getExpiryDate().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Liên kết đặt lại mật khẩu không hợp lệ, đã hết hạn hoặc đã được sử dụng."));

        User user = token.getUser();
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new IllegalArgumentException("Không thể đặt lại mật khẩu cho tài khoản này.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        token.setIsUsed(true);
        tokenRepository.save(token);
    }

    private String createRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
