package com.example.shoptech.service;

import com.example.shoptech.entity.User;
import com.example.shoptech.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameAndIsDeletedFalse(usernameOrEmail)
                .orElseGet(() -> userRepository.findByEmailAndIsDeletedFalse(usernameOrEmail)
                        .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + usernameOrEmail)));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getIsActive(),       // enabled
                true,                    // accountNonExpired
                true,                    // credentialsNonExpired
                !user.getIsDeleted(),    // accountNonLocked
                user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toList())
        );
    }
}
