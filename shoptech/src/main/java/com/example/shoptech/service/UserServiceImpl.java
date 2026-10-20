package com.example.shoptech.service;

import com.example.shoptech.dto.RegisterDTO;
import com.example.shoptech.dto.AdminUserForm;
import com.example.shoptech.entity.Role;
import com.example.shoptech.entity.User;
import com.example.shoptech.repository.RoleRepository;
import com.example.shoptech.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User registerUser(RegisterDTO registerDTO) {
        if (userRepository.existsByUsername(registerDTO.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại!");
        }
        if (userRepository.existsByEmail(registerDTO.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng!");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name("ROLE_USER")
                        .description("Khách hàng mua sắm thành viên")
                        .build()));

        User user = User.builder()
                .username(registerDTO.getUsername())
                .email(registerDTO.getEmail())
                .password(passwordEncoder.encode(registerDTO.getPassword()))
                .fullName(registerDTO.getFullName())
                .phoneNumber(registerDTO.getPhoneNumber())
                .address(registerDTO.getAddress())
                .isActive(true)
                .isDeleted(false)
                .roles(Collections.singleton(userRole))
                .build();

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateUser(Long id, AdminUserForm form) {
        User user = userRepository.findById(id)
                .filter(existingUser -> !Boolean.TRUE.equals(existingUser.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng ID: " + id));

        if (userRepository.existsByUsernameAndIdNot(form.getUsername(), id)) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại!");
        }
        if (userRepository.existsByEmailAndIdNot(form.getEmail(), id)) {
            throw new IllegalArgumentException("Email đã được sử dụng!");
        }

        user.setUsername(form.getUsername().trim());
        user.setEmail(form.getEmail().trim());
        user.setFullName(form.getFullName().trim());
        user.setPhoneNumber(form.getPhoneNumber());
        user.setAddress(form.getAddress());
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(form.getPassword()));
        }

        userRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsernameAndIsDeletedFalse(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailAndIsDeletedFalse(email);
    }

    @Override
    public Page<User> getActiveUsers(String keyword, Pageable pageable) {
        String searchTerm = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return userRepository.searchActiveUsers(searchTerm, pageable);
    }

    @Override
    public Page<User> getTrashUsers(Pageable pageable) {
        return userRepository.searchTrashUsers(null, pageable);
    }

    @Override
    @Transactional
    public void toggleActiveStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng ID: " + id));
        user.setIsActive(!Boolean.TRUE.equals(user.getIsActive()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void moveToTrash(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng ID: " + id));
        user.setIsDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void restoreFromTrash(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng ID: " + id));
        user.setIsDeleted(false);
        user.setDeletedAt(null);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void forceDelete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng ID: " + id));
        userRepository.delete(user);
    }
}
