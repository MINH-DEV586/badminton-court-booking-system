package com.badmintonhub.backend.service;

import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.badmintonhub.backend.dto.response.CourtResponse;
import com.badmintonhub.backend.exception.ApiException;
import com.badmintonhub.backend.repository.CourtRepository;
import com.badmintonhub.backend.config.CacheConfig;

@Service
public class CourtService {
    private final CourtRepository courts;

    public CourtService(CourtRepository courtRepository) {
        this.courts = courtRepository;
    }

    @Cacheable(cacheNames = CacheConfig.COURT_DETAILS, key = "#p0")
    @Transactional(readOnly = true)
    public CourtResponse getCourt(UUID id) {
        var court = courts.findById(id).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "COURT_NOT_FOUND", "Court not found"));
        return CourtResponse.from(court);
    }
}
