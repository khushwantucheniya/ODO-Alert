package com.odoalert.service;

import com.odoalert.repo.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository users;
    public CustomUserDetailsService(UserRepository users) { this.users = users; }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var user = users.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new org.springframework.security.core.userdetails.User(
            user.getEmail(), user.getPasswordHash(), List.of()
        );
    }
}
