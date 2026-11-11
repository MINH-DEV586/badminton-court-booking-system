package com.badmintonhub.backend.security;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
    @NotBlank String jwtSecret,
    @DefaultValue("badmintonhub") @NotBlank String issuer,
    @DefaultValue("15m") Duration accessTokenTtl,
    @DefaultValue("7d") Duration refreshTokenTtl,
    @DefaultValue({"http://localhost:5173", "http://localhost:3000"}) List<String> allowedOrigins
) {
    @AssertTrue(message = "Token lifetimes must be positive and access token lifetime must be shorter than refresh token lifetime")
    public boolean isTokenLifetimeValid() {
        return accessTokenTtl != null && refreshTokenTtl != null
            && accessTokenTtl.toSeconds() > 0 && refreshTokenTtl.toSeconds() > 0
            && accessTokenTtl.compareTo(refreshTokenTtl) < 0;
    }
}
