package com.badmintonhub.backend.auth;

import com.badmintonhub.backend.entity.UserAccount;
import com.badmintonhub.backend.exception.ApiException;
import com.badmintonhub.backend.repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokens;

    public AuthService(UserAccountRepository users, PasswordEncoder encoder,
                       AuthenticationManager authenticationManager, TokenService tokens) {
        this.users = users;
        this.encoder = encoder;
        this.authenticationManager = authenticationManager;
        this.tokens = tokens;
    }

    @Transactional
    public AuthDtos.TokenResponse register(AuthDtos.RegisterRequest request) {
        var email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw emailAlreadyExists();
        }
        var user = new UserAccount();
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(request.password()));
        user.setFullName(request.fullName().strip());
        user.setPhone(request.phone() == null ? null : request.phone().strip());
        // Public registration never accepts privileged roles from the client.
        user.setRole(UserAccount.Role.CUSTOMER);
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            // The unique database constraint also handles concurrent registration requests.
            throw emailAlreadyExists();
        }
        return tokens.issue(user);
    }

    @Transactional
    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest request) {
        var email = normalizeEmail(request.email());
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
        var user = users.findByEmail(email).filter(UserAccount::isActive)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password"));
        return tokens.issue(user);
    }

    @Transactional(readOnly = true)
    public AuthDtos.UserResponse currentUser(UUID id) {
        return users.findById(id).filter(UserAccount::isActive).map(AuthDtos.UserResponse::from)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required"));
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static ApiException emailAlreadyExists() {
        return new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "Email is already registered");
    }
}
