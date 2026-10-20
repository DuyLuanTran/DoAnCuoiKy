package com.example.shoptech.service;

import com.example.shoptech.dto.RegisterDTO;
import com.example.shoptech.dto.AdminUserForm;
import com.example.shoptech.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserService {

    User registerUser(RegisterDTO registerDTO);

    void updateUser(Long id, AdminUserForm form);

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Page<User> getActiveUsers(String keyword, Pageable pageable);

    Page<User> getTrashUsers(Pageable pageable);

    void toggleActiveStatus(Long id);

    void moveToTrash(Long id);

    void restoreFromTrash(Long id);

    void forceDelete(Long id);
}
