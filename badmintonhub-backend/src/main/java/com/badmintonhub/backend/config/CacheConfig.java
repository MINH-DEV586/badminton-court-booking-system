package com.badmintonhub.backend.config;

import com.badmintonhub.backend.dto.response.CourtResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.cache.autoconfigure.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
@PropertySource("classpath:redis-cache.properties")
public class CacheConfig {
    public static final String COURT_DETAILS = "courtDetails";

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, CacheProperties properties) {
        var ttl = properties.getRedis().getTimeToLive();
        if (ttl == null) {
            ttl = Duration.ofMinutes(10);
        }
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Redis cache TTL must be positive");
        }
        var configuration = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(ttl)
            .disableCachingNullValues()
            .computePrefixWith(name -> "badmintonhub:v1:" + name + "::")
            .serializeValuesWith(SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(CourtResponse.class)));
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(configuration)
            .withInitialCacheConfigurations(Map.of(COURT_DETAILS, configuration))
            .disableCreateOnMissingCache()
            .transactionAware()
            .build();
    }
}
