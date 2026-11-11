package com.badmintonhub.backend.auth;

import com.badmintonhub.backend.entity.RefreshToken;
import com.badmintonhub.backend.entity.UserAccount;
import com.badmintonhub.backend.exception.ApiException;
import com.badmintonhub.backend.repository.RefreshTokenRepository;
import com.badmintonhub.backend.security.SecurityProperties;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class TokenService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final RefreshTokenRepository refreshTokens;
    private final JwtEncoder encoder;
    private final SecurityProperties properties;
    private final Clock clock;

    public TokenService(RefreshTokenRepository refreshTokens, JwtEncoder encoder,
                        SecurityProperties properties, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public AuthDtos.TokenResponse issue(UserAccount user) {
        if (!user.isActive()) {
            throw invalidRefreshToken();
        }
        var now = clock.instant();
        var claims = JwtClaimsSet.builder()
            .issuer(properties.issuer()).subject(user.getId().toString())
            .audience(List.of("badmintonhub-api"))
            .issuedAt(now).expiresAt(now.plus(properties.accessTokenTtl()))
            .id(UUID.randomUUID().toString())
            .claim("token_use", "access").claim("role", user.getRole().name()).build();
        var access = encoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        var randomBytes = new byte[32];
        RANDOM.nextBytes(randomBytes);
        var rawRefresh = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        var refresh = new RefreshToken();
        refresh.setTokenHash(hash(rawRefresh));
        refresh.setUser(user);
        refresh.setExpiresAt(now.plus(properties.refreshTokenTtl()));
        refreshTokens.save(refresh);
        return new AuthDtos.TokenResponse(access, rawRefresh, "Bearer",
            properties.accessTokenTtl().toSeconds(), AuthDtos.UserResponse.from(user));
    }

    @Transactional
    public AuthDtos.TokenResponse refresh(String rawToken) {
        var token = refreshTokens.findByHashForUpdate(hash(rawToken)).orElseThrow(TokenService::invalidRefreshToken);
        if (token.getRevokedAt() != null || !token.getExpiresAt().isAfter(clock.instant())
            || !token.getUser().isActive()) {
            throw invalidRefreshToken();
        }
        token.setRevokedAt(clock.instant());
        return issue(token.getUser());
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokens.findByHashForUpdate(hash(rawToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(clock.instant());
            }
        });
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static ApiException invalidRefreshToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Invalid or expired refresh token");
    }
}
