package com.badmintonhub.backend.security;

import com.badmintonhub.backend.entity.UserAccount;
import com.badmintonhub.backend.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SecurityConfigTests.TestConfig.class)
@WebAppConfiguration
@TestPropertySource(properties = "app.security.jwt-secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class SecurityConfigTests {
    static final UUID ID = UUID.randomUUID();
    @Autowired WebApplicationContext context;
    @Autowired UserAccountRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired AuthenticationManager authenticationManager;
    @Autowired JwtEncoder jwtEncoder;
    MockMvc mvc;
    UserAccount account;

    @BeforeEach
    void setUp() {
        reset(users);
        account = new UserAccount();
        account.setEmail("customer@example.com");
        account.setRole(UserAccount.Role.CUSTOMER);
        when(users.findById(ID)).thenReturn(Optional.of(account));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void anonymousRequestGetsJson401() throws Exception {
        mvc.perform(get("/api/private")).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void customerCannotAccessOwnerOrAdmin() throws Exception {
        var token = token("badmintonhub", "access", Instant.now().plusSeconds(900));
        mvc.perform(get("/api/owner/probe").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/probe").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanAccessOwnerButNotAdmin() throws Exception {
        account.setRole(UserAccount.Role.OWNER);
        var token = token("badmintonhub", "access", Instant.now().plusSeconds(900));
        mvc.perform(get("/api/owner/probe").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk());
        mvc.perform(get("/api/admin/probe").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessBothAreas() throws Exception {
        account.setRole(UserAccount.Role.ADMIN);
        var token = token("badmintonhub", "access", Instant.now().plusSeconds(900));
        mvc.perform(get("/api/owner/probe").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk());
        mvc.perform(get("/api/admin/probe").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void permissionsComeFromDatabaseAndDisabledAccountsAreRejected() throws Exception {
        var token = token("badmintonhub", "access", Instant.now().plusSeconds(900));
        // Even a signed ADMIN claim must not override the current CUSTOMER role.
        mvc.perform(get("/api/admin/probe").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isForbidden());
        account.setActive(false);
        mvc.perform(get("/api/private").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidExpiredAndWrongPurposeTokensAreRejected() throws Exception {
        for (var token : List.of("invalid", token("wrong-issuer", "access", Instant.now().plusSeconds(900)),
            token("badmintonhub", "refresh", Instant.now().plusSeconds(900)),
            token("badmintonhub", "access", Instant.now().minusSeconds(120)))) {
            mvc.perform(get("/api/private").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void bearerAuthenticationIsStateless() throws Exception {
        var token = token("badmintonhub", "access", Instant.now().plusSeconds(900));
        var result = mvc.perform(post("/api/private").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk()).andReturn();
        assertNull(result.getRequest().getSession(false));
        mvc.perform(get("/api/private")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginAuthenticatesBcryptPasswordAndRejectsWrongOrDisabledCredentials() {
        account.setPasswordHash(encoder.encode("secret123"));
        assertNotEquals("secret123", account.getPasswordHash());
        when(users.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        assertTrue(authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
            " CUSTOMER@EXAMPLE.COM ", "secret123")).isAuthenticated());
        assertThrows(AuthenticationException.class, () -> authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated("customer@example.com", "wrong")));
        account.setActive(false);
        assertThrows(AuthenticationException.class, () -> authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated("customer@example.com", "secret123")));
    }

    @Test
    void corsAllowsConfiguredFrontendAndRejectsOtherOrigins() throws Exception {
        mvc.perform(options("/api/private").header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
        mvc.perform(options("/api/private").header(HttpHeaders.ORIGIN, "https://untrusted.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .andExpect(status().isForbidden());
    }

    private String token(String issuer, String purpose, Instant expiry) {
        var claims = JwtClaimsSet.builder().issuer(issuer).subject(ID.toString())
            .audience(List.of("badmintonhub-api")).issuedAt(Instant.now().minusSeconds(300))
            .expiresAt(expiry).claim("token_use", purpose).claim("role", "ADMIN").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, JwtConfig.class, AccountJwtAuthenticationConverter.class,
        DatabaseUserDetailsService.class, ProbeController.class})
    static class TestConfig {
        @Bean UserAccountRepository users() { return mock(UserAccountRepository.class); }
    }

    @RestController
    static class ProbeController {
        @GetMapping({"/api/private", "/api/owner/probe", "/api/admin/probe"})
        String read() { return "ok"; }
        @PostMapping("/api/private")
        String write() { return "ok"; }
    }
}
