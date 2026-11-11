package com.badmintonhub.backend.security;

import com.badmintonhub.backend.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserAccountRepository users;

    public DatabaseUserDetailsService(UserAccountRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        var account = users.findByEmail(email.strip().toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        return User.withUsername(account.getEmail())
            .password(account.getPasswordHash())
            .roles(account.getRole().name())
            .disabled(!account.isActive())
            .build();
    }
}
