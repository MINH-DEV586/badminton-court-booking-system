package com.badmintonhub.backend.auth;

import com.badmintonhub.backend.entity.RefreshToken;
import com.badmintonhub.backend.entity.UserAccount;
import com.badmintonhub.backend.repository.RefreshTokenRepository;
import com.badmintonhub.backend.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserAccountRepository users;
    @Autowired RefreshTokenRepository refreshTokens;
    @Autowired PasswordEncoder encoder;
    @Autowired TokenService tokens;

    @BeforeEach
    void cleanDatabase() {
        refreshTokens.deleteAll();
        users.deleteAll();
    }

    @Test
    void registerHashesPasswordNormalizesEmailAndPreventsRoleInjection() throws Exception {
        var result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"Customer@Example.com","password":"secret123","fullName":" Customer ","role":"ADMIN"}
                    """))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.user.role").value("CUSTOMER"))
            .andExpect(jsonPath("$.user.email").value("customer@example.com"))
            .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(900))
            .andExpect(header().string("Cache-Control", "no-store")).andReturn();
        var account = users.findByEmail("customer@example.com").orElseThrow();
        assertTrue(encoder.matches("secret123", account.getPasswordHash()));
        assertEquals("Customer", account.getFullName());
        var body = mapper.readTree(result.getResponse().getContentAsString());
        assertNotEquals(body.get("refreshToken").asText(), refreshTokens.findAll().getFirst().getTokenHash());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + body.get("accessToken").asText()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("customer@example.com"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        register();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registrationJson("CUSTOMER@EXAMPLE.COM", "secret123")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
        assertEquals(1, users.count());
    }

    @Test
    void loginReturnsTokensAndRejectsWrongUnknownAndDisabledAccounts() throws Exception {
        register();
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("CUSTOMER@EXAMPLE.COM", "secret123")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty());
        for (var email : new String[]{"customer@example.com", "missing@example.com"}) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson(email, "wrong")))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }
        var account = users.findByEmail("customer@example.com").orElseThrow();
        account.setActive(false);
        users.saveAndFlush(account);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("customer@example.com", "secret123")))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void refreshRotatesTokenAndRejectsReuseAndLogoutRevokesReplacement() throws Exception {
        var first = register();
        var oldRefresh = first.get("refreshToken").asText();
        var result = mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(refreshJson(oldRefresh)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty()).andReturn();
        var replacement = mapper.readTree(result.getResponse().getContentAsString()).get("refreshToken").asText();
        assertNotEquals(oldRefresh, replacement);
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshJson(oldRefresh)))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON).content(refreshJson(replacement)))
            .andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshJson(replacement)))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON).content(refreshJson(replacement)))
            .andExpect(status().isNoContent());
    }

    @Test
    void concurrentRefreshAllowsOnlyOneSuccessfulRotation() throws Exception {
        var raw = register().get("refreshToken").asText();
        var gate = new CountDownLatch(1);
        Callable<Boolean> rotate = () -> {
            gate.await();
            try {
                tokens.refresh(raw);
                return true;
            } catch (com.badmintonhub.backend.exception.ApiException exception) {
                assertEquals("INVALID_REFRESH_TOKEN", exception.getCode());
                return false;
            }
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(rotate);
            var second = executor.submit(rotate);
            gate.countDown();
            assertNotEquals(first.get(10, java.util.concurrent.TimeUnit.SECONDS),
                second.get(10, java.util.concurrent.TimeUnit.SECONDS));
        }
        assertEquals(2, refreshTokens.count());
    }

    @Test
    void expiredUnknownAndDisabledRefreshTokensAreRejected() throws Exception {
        var first = register();
        var account = users.findByEmail("customer@example.com").orElseThrow();
        var rawExpired = "A".repeat(43);
        var expired = new RefreshToken();
        expired.setUser(account);
        expired.setTokenHash(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(rawExpired.getBytes(StandardCharsets.UTF_8))));
        expired.setExpiresAt(Instant.now().minusSeconds(1));
        refreshTokens.saveAndFlush(expired);
        for (var raw : new String[]{rawExpired, "B".repeat(43)}) {
            mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshJson(raw)))
                .andExpect(status().isUnauthorized());
        }
        account.setActive(false);
        users.saveAndFlush(account);
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(refreshJson(first.get("refreshToken").asText())))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + first.get("accessToken").asText()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidRegistrationIsRejectedWithoutPersistingUser() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"invalid\",\"password\":\"short\",\"fullName\":\" \"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.errors.email").exists()).andExpect(jsonPath("$.errors.password").exists())
            .andExpect(jsonPath("$.errors.fullName").exists());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registrationJson("customer@example.com", "ệ".repeat(25))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.password").exists());
        assertEquals(0, users.count());
    }

    @Test
    void malformedJsonAndTokenFormatReturnSafeErrors() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.trace").doesNotExist());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshJson("bad")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    private JsonNode register() throws Exception {
        var result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registrationJson("customer@example.com", "secret123")))
            .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private String registrationJson(String email, String password) {
        return mapper.writeValueAsString(Map.of("email", email, "password", password, "fullName", "Customer"));
    }

    private String loginJson(String email, String password) {
        return mapper.writeValueAsString(Map.of("email", email, "password", password));
    }

    private String refreshJson(String token) {
        return mapper.writeValueAsString(Map.of("refreshToken", token));
    }
}
