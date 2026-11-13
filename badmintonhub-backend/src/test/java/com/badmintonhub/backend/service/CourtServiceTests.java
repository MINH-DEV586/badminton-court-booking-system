package com.badmintonhub.backend.service;

import com.badmintonhub.backend.config.CacheConfig;
import com.badmintonhub.backend.entity.Court;
import com.badmintonhub.backend.entity.Venue;
import com.badmintonhub.backend.exception.ApiException;
import com.badmintonhub.backend.repository.CourtRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(CourtServiceTests.TestConfig.class)
class CourtServiceTests {
    @Autowired CourtService service;
    @Autowired CourtRepository courts;
    @Autowired CacheManager cacheManager;

    @BeforeEach
    void resetCache() {
        reset(courts);
        cacheManager.getCache(CacheConfig.COURT_DETAILS).clear();
    }

    @Test
    void repeatedReadUsesCacheAndDifferentIdsUseDifferentKeys() {
        var first = court(UUID.randomUUID());
        var second = court(UUID.randomUUID());
        when(courts.findById(first.getId())).thenReturn(Optional.of(first));
        when(courts.findById(second.getId())).thenReturn(Optional.of(second));
        assertEquals(service.getCourt(first.getId()), service.getCourt(first.getId()));
        assertEquals(second.getId(), service.getCourt(second.getId()).id());
        verify(courts, times(1)).findById(first.getId());
        verify(courts, times(1)).findById(second.getId());
    }

    @Test
    void missingCourtIsNotCached() {
        var id = UUID.randomUUID();
        when(courts.findById(id)).thenReturn(Optional.empty());
        assertEquals("COURT_NOT_FOUND", assertThrows(ApiException.class, () -> service.getCourt(id)).getCode());
        assertThrows(ApiException.class, () -> service.getCourt(id));
        verify(courts, times(2)).findById(id);
    }

    private Court court(UUID id) {
        var venue = new Venue();
        ReflectionTestUtils.setField(venue, "id", UUID.randomUUID());
        var court = new Court();
        ReflectionTestUtils.setField(court, "id", id);
        court.setVenue(venue);
        court.setName("Court A");
        court.setBaseHourlyRateVnd(120000);
        return court;
    }

    @Configuration
    @EnableCaching
    @Import(CourtService.class)
    static class TestConfig {
        @Bean CourtRepository courts() { return mock(CourtRepository.class); }
        @Bean CacheManager cacheManager() { return new ConcurrentMapCacheManager(CacheConfig.COURT_DETAILS); }
    }
}
