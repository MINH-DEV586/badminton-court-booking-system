package com.badmintonhub.backend.service;

import com.badmintonhub.backend.auth.AuthDtos;
import com.badmintonhub.backend.dto.request.UpdateProfileRequest;
import com.badmintonhub.backend.entity.UserAccount;
import com.badmintonhub.backend.exception.ApiException;
import com.badmintonhub.backend.repository.UserAccountRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Service
@Validated
public class UserService {
    private final UserAccountRepository users;

    public UserService(UserAccountRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public AuthDtos.UserResponse getProfile(@NotNull UUID id) {
        checkUserOrAdminAccess(id);
        var user = users.findById(id).filter(UserAccount::isActive)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        return AuthDtos.UserResponse.from(user);
    }

    @Transactional
    public AuthDtos.UserResponse updateProfile(@NotNull UUID id, @NotNull @Valid UpdateProfileRequest request) {
        checkUserOrAdminAccess(id);
        var user = users.findById(id).filter(UserAccount::isActive)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        user.setFullName(request.fullName().strip());
        user.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().strip());

        return AuthDtos.UserResponse.from(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AuthDtos.UserResponse updateUserRole(@NotNull UUID id, @NotNull UserAccount.Role role) {
        var user = users.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        user.setRole(role);
        return AuthDtos.UserResponse.from(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AuthDtos.UserResponse setUserActive(@NotNull UUID id, boolean active) {
        var user = users.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        user.setActive(active);
        return AuthDtos.UserResponse.from(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Page<AuthDtos.UserResponse> getAllUsers(Pageable pageable) {
        return users.findAll(pageable).map(AuthDtos.UserResponse::from);
    }

    private void checkUserOrAdminAccess(UUID id) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var admin = auth.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && !id.toString().equals(auth.getName())) {
            throw new AccessDeniedException("Not authorized to access or modify this profile");
        }
    }
}
