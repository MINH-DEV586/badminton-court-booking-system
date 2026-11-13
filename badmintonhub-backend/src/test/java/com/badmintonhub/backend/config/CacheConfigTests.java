package com.badmintonhub.backend.config;

import com.badmintonhub.backend.dto.response.CourtResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.cache.autoconfigure.CacheProperties;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CacheConfigTests {
    @Test
    void usesTypedJsonWithExpiryAndTransactionAwareInvalidation() {
        var properties = new CacheProperties();
        properties.getRedis().setTimeToLive(Duration.ofSeconds(45));
        var manager = new CacheConfig().cacheManager(mock(RedisConnectionFactory.class), properties);
        manager.afterPropertiesSet();
        var cache = (org.springframework.cache.transaction.TransactionAwareCacheDecorator)
            manager.getCache(CacheConfig.COURT_DETAILS);
        var config = ((org.springframework.data.redis.cache.RedisCache) cache.getTargetCache()).getCacheConfiguration();
        var value = new CourtResponse(UUID.randomUUID(), UUID.randomUUID(), "Court A", 120000, true, Instant.now());
        var serialized = config.getValueSerializationPair().write(value);
        var restored = config.getValueSerializationPair().read(serialized);
        assertEquals(value, restored);
        assertEquals(Duration.ofSeconds(45), config.getTtlFunction().getTimeToLive("id", value));
        assertFalse(config.getAllowCacheNullValues());
        assertTrue(manager.isTransactionAware());
        assertEquals("badmintonhub:v1:courtDetails::", config.getKeyPrefixFor(CacheConfig.COURT_DETAILS));
        assertNull(manager.getCache("unknown-cache"));
    }

    @Test
    void rejectsUnlimitedOrNegativeTtl() {
        var properties = new CacheProperties();
        for (var ttl : new Duration[]{Duration.ZERO, Duration.ofSeconds(-1)}) {
            properties.getRedis().setTimeToLive(ttl);
            assertThrows(IllegalArgumentException.class, () ->
                new CacheConfig().cacheManager(mock(RedisConnectionFactory.class), properties));
        }
    }
}
