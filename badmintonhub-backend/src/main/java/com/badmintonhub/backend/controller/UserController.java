package com.badmintonhub.backend.controller;

import com.badmintonhub.backend.dto.request.*;
import com.badmintonhub.backend.dto.response.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.*;

import com.badmintonhub.backend.auth.AuthDtos;
import com.badmintonhub.backend.entity.UserAccount;
import com.badmintonhub.backend.service.UserService;
import org.springframework.http.CacheControl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api")
public class UserController {
    private final UserService users;

    public UserController(UserService users) { this.users = users; }

    @GetMapping("/users/me")
    public ResponseEntity<AuthDtos.UserResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(users.getProfile(UUID.fromString(jwt.getSubject())));
    }

    @PutMapping("/users/me")
    public ResponseEntity<AuthDtos.UserResponse> updateMyProfile(@AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(users.updateProfile(UUID.fromString(jwt.getSubject()), request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    public ResponseEntity<PagedModel<AuthDtos.UserResponse>> getUsers(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new PagedModel<>(
            users.getAllUsers(PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users/{id}")
    public ResponseEntity<AuthDtos.UserResponse> getProfile(@PathVariable UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(users.getProfile(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/users/{id}/role")
    public ResponseEntity<AuthDtos.UserResponse> updateRole(@PathVariable UUID id,
        @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(users.updateUserRole(id, request.role()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/users/{id}/active")
    public ResponseEntity<AuthDtos.UserResponse> updateActive(@PathVariable UUID id,
        @Valid @RequestBody UpdateActiveRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(users.setUserActive(id, request.active()));
    }

    public record UpdateRoleRequest(@NotNull UserAccount.Role role) {}
    public record UpdateActiveRequest(@NotNull Boolean active) {}
}
