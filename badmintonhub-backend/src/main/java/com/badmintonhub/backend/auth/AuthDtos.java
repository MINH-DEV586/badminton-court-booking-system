package com.badmintonhub.backend.auth;

import com.badmintonhub.backend.entity.UserAccount;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) @Utf8Password String password,
        @NotBlank @Size(max = 120) String fullName,
        @Size(max = 30) String phone
    ) {}

    public record LoginRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 72) @Utf8Password String password
    ) {}

    public record RefreshRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "Invalid refresh token format") String refreshToken
    ) {}

    public record UserResponse(UUID id, String email, String fullName, String phone, UserAccount.Role role) {
        public static UserResponse from(UserAccount user) {
            return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getPhone(), user.getRole());
        }
    }

    public record TokenResponse(String accessToken, String refreshToken, String tokenType,
                                long expiresIn, UserResponse user) {}
}
