package com.badmintonhub.backend.service;

import com.badmintonhub.backend.dto.request.UpdateCourtRequest;
import com.badmintonhub.backend.entity.*;
import com.badmintonhub.backend.exception.ApiException;
import com.badmintonhub.backend.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.cache.type=redis",
    "spring.cache.redis.time-to-live=30s",
    "spring.datasource.url=jdbc:h2:mem:redis-cache-integration;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "redis.integration.enabled", matches = "true")
class RedisCourtCacheIntegrationTests {
    @Autowired CourtService service;
    @Autowired MockMvc mvc;
    @Autowired StringRedisTemplate redis;
    @MockitoSpyBean CourtRepository courts;
    @Autowired VenueRepository venues;
    @Autowired UserAccountRepository users;
    @Autowired BookingRepository bookings;
    @Autowired TransactionTemplate transactions;
    @Autowired org.springframework.cache.CacheManager cacheManager;
    Court court;
    UserAccount owner;
    String key;

    @BeforeEach
    void setUp() {
        bookings.deleteAll();
        courts.deleteAll();
        venues.deleteAll();
        users.deleteAll();
        owner = new UserAccount();
        owner.setEmail("cache-owner@example.com");
        owner.setPasswordHash("not-used-in-this-test");
        owner.setFullName("Cache owner");
        owner.setRole(UserAccount.Role.OWNER);
        users.saveAndFlush(owner);
        var venue = new Venue();
        venue.setOwner(owner);
        venue.setName("Cache test venue");
        venue.setAddress("Test address");
        venue.setTimeZone("Asia/Ho_Chi_Minh");
        venues.saveAndFlush(venue);
        court = new Court();
        court.setVenue(venue);
        court.setName("Court before update");
        court.setBaseHourlyRateVnd(120000);
        courts.saveAndFlush(court);
        key = "badmintonhub:v1:courtDetails::" + court.getId();
        clearInvocations(courts);
        var region = (org.springframework.cache.transaction.TransactionAwareCacheDecorator)
            cacheManager.getCache("courtDetails");
        var redisConfig = ((org.springframework.data.redis.cache.RedisCache) region.getTargetCache()).getCacheConfiguration();
        assertEquals(Duration.ofSeconds(30), redisConfig.getTtlFunction().getTimeToLive("id", null),
            "The integration context must own its 30-second Redis configuration");
    }

    @AfterEach
    void cleanOwnCacheKey() {
        // Never flush the shared Redis database or keys belonging to another application.
        if (key != null) {
            redis.delete(key);
        }
        SecurityContextHolder.clearContext();
    }

    @Test
    void readsThroughApiUseRealRedisAndDoNotBypassAuthentication() throws Exception {
        for (int read = 0; read < 2; read++) {
            mvc.perform(get("/api/courts/{id}", court.getId()).with(identity(owner.getId(), "CUSTOMER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Court before update"))
                .andExpect(jsonPath("$.venueId").value(court.getVenue().getId().toString()));
        }
        verify(courts, times(1)).findById(court.getId());
        var json = redis.opsForValue().get(key);
        assertNotNull(json);
        assertTrue(json.startsWith("{"));
        assertFalse(json.contains("passwordHash"));
        mvc.perform(get("/api/courts/{id}", court.getId())).andExpect(status().isUnauthorized());
    }

    @Test
    void redisHasConfiguredTtlAndExpiredEntryIsReloaded() {
        service.getCourt(court.getId());
        var ttl = redis.getExpire(key, TimeUnit.MILLISECONDS);
        assertNotNull(ttl);
        assertTrue(ttl > 0 && ttl <= 30000, "Actual Redis TTL in milliseconds: " + ttl);
        redis.expire(key, Duration.ofMillis(100));
        await().atMost(Duration.ofSeconds(3)).until(() -> Boolean.FALSE.equals(redis.hasKey(key)));
        service.getCourt(court.getId());
        verify(courts, times(2)).findById(court.getId());
    }

    @Test
    void ownerUpdateCommitsDatabaseThenEvictsCache() throws Exception {
        service.getCourt(court.getId());
        mvc.perform(put("/api/owner/courts/{id}", court.getId()).with(identity(owner.getId(), "OWNER"))
                .contentType(MediaType.APPLICATION_JSON).content(updateJson()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.baseHourlyRateVnd").value(150000));
        assertFalse(redis.hasKey(key));
        assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive(),
            "HTTP update must finish its transaction before the next service read");
        clearInvocations(courts);
        assertEquals(150000, service.getCourt(court.getId()).baseHourlyRateVnd());
        verify(courts, times(1)).findById(court.getId());
        assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive(),
            "Read transaction must have committed the cache put");
        var refreshed = redis.opsForValue().get(key);
        assertNotNull(refreshed, "A cache miss must repopulate Redis after the database read");
        assertTrue(refreshed.contains("150000"));
    }

    @Test
    void rollbackKeepsOriginalDatabaseAndCacheValues() {
        var before = service.getCourt(court.getId());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            owner.getId().toString(), "", List.of(new SimpleGrantedAuthority("ROLE_OWNER"))));
        transactions.executeWithoutResult(transaction -> {
            service.updateCourt(court.getId(), new UpdateCourtRequest("Rolled back", 999000L, false));
            assertTrue(redis.hasKey(key));
            transaction.setRollbackOnly();
        });
        assertEquals(before, service.getCourt(court.getId()));
        assertEquals(120000, courts.findById(court.getId()).orElseThrow().getBaseHourlyRateVnd());
    }

    @Test
    void differentOwnerCannotUpdateOrDeleteCourt() throws Exception {
        service.getCourt(court.getId());
        var otherOwner = UUID.randomUUID();
        mvc.perform(put("/api/owner/courts/{id}", court.getId()).with(identity(otherOwner, "OWNER"))
                .contentType(MediaType.APPLICATION_JSON).content(updateJson())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/owner/courts/{id}", court.getId()).with(identity(otherOwner, "OWNER")))
            .andExpect(status().isForbidden());
        assertTrue(redis.hasKey(key));
        assertEquals(120000, courts.findById(court.getId()).orElseThrow().getBaseHourlyRateVnd());
    }

    @Test
    void administratorCanManageCourtOwnedByAnotherAccount() throws Exception {
        service.getCourt(court.getId());
        mvc.perform(put("/api/owner/courts/{id}", court.getId()).with(identity(UUID.randomUUID(), "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(updateJson())).andExpect(status().isOk());
        assertFalse(redis.hasKey(key));
    }

    @Test
    void validationAndCustomerRoleRejectWritesWithoutEvicting() throws Exception {
        service.getCourt(court.getId());
        mvc.perform(put("/api/owner/courts/{id}", court.getId()).with(identity(owner.getId(), "CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content(updateJson())).andExpect(status().isForbidden());
        mvc.perform(put("/api/owner/courts/{id}", court.getId()).with(identity(owner.getId(), "OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\" \",\"baseHourlyRateVnd\":-1,\"active\":true}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        assertTrue(redis.hasKey(key));
    }

    @Test
    void successfulDeleteEvictsAndSubsequentReadReturnsNotFound() throws Exception {
        service.getCourt(court.getId());
        mvc.perform(delete("/api/owner/courts/{id}", court.getId()).with(identity(owner.getId(), "OWNER")))
            .andExpect(status().isNoContent());
        assertFalse(redis.hasKey(key));
        assertEquals("COURT_NOT_FOUND", assertThrows(ApiException.class, () -> service.getCourt(court.getId())).getCode());
        assertFalse(redis.hasKey(key));
    }

    @Test
    void failedDeleteWithExistingBookingKeepsCache() throws Exception {
        var booking = new Booking();
        booking.setCustomer(owner);
        booking.setCourt(court);
        booking.setStartsAt(Instant.now().plusSeconds(3600));
        booking.setEndsAt(Instant.now().plusSeconds(7200));
        booking.setTotalAmountVnd(120000);
        booking.setStatus(Booking.Status.CONFIRMED);
        bookings.saveAndFlush(booking);
        service.getCourt(court.getId());
        mvc.perform(delete("/api/owner/courts/{id}", court.getId()).with(identity(owner.getId(), "OWNER")))
            .andExpect(status().isConflict());
        assertTrue(redis.hasKey(key));
        assertTrue(courts.existsById(court.getId()));
    }

    private RequestPostProcessor identity(UUID id, String role) {
        return jwt().jwt(token -> token.subject(id.toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private String updateJson() {
        return "{\"name\":\"Court after update\",\"baseHourlyRateVnd\":150000,\"active\":false}";
    }
}
