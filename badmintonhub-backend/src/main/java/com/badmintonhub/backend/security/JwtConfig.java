package com.badmintonhub.backend.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.util.Base64;

@Configuration
public class JwtConfig {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public SecretKey jwtSigningKey(SecurityProperties properties) {
        byte[] key;
        try {
            key = Base64.getDecoder().decode(properties.jwtSecret());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("app.security.jwt-secret must be Base64 encoded", exception);
        }
        if (key.length < 32) {
            throw new IllegalArgumentException("app.security.jwt-secret must contain at least 32 random bytes");
        }
        return new SecretKeySpec(key, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSigningKey, SecurityProperties properties) {
        var decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(properties.issuer()),
            new JwtClaimValidator<String>("token_use", "access"::equals),
            new JwtClaimValidator<java.util.List<String>>("aud", audience ->
                audience != null && audience.contains("badmintonhub-api")),
            new JwtClaimValidator<java.time.Instant>("exp", expiry -> expiry != null)));
        return decoder;
    }
}
