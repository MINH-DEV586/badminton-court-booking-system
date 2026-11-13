package com.badmintonhub.backend.service;

import java.util.UUID;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.badmintonhub.backend.dto.response.CourtResponse;
import com.badmintonhub.backend.exception.ApiException;
import com.badmintonhub.backend.repository.CourtRepository;
import com.badmintonhub.backend.config.CacheConfig;
import com.badmintonhub.backend.dto.request.UpdateCourtRequest;
import com.badmintonhub.backend.entity.Court;

@Service
@Validated
public class CourtService {
    private final CourtRepository courts;
    private final Cache courtCache;

    public CourtService(CourtRepository courtRepository, CacheManager cacheManager) {
        this.courts = courtRepository;
        this.courtCache = java.util.Objects.requireNonNull(cacheManager.getCache(CacheConfig.COURT_DETAILS),
            "Court details cache must be configured");
    }

    @Transactional(readOnly = true)
    public CourtResponse getCourt(UUID id) {
        var cached = courtCache.get(id, CourtResponse.class);
        if (cached != null) {
            return cached;
        }
        var court = courts.findById(id).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "COURT_NOT_FOUND", "Court not found"));
        var response = CourtResponse.from(court);
        courtCache.put(id, response);
        return response;
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Transactional
    public CourtResponse updateCourt(@NotNull UUID id, @NotNull @Valid UpdateCourtRequest request) {
        var court = managedCourt(id);
        court.setName(request.name().strip());
        court.setBaseHourlyRateVnd(request.baseHourlyRateVnd());
        court.setActive(request.active());
        courtCache.evict(id);
        return CourtResponse.from(court);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Transactional
    public void deleteCourt(@NotNull UUID id) {
        courts.delete(managedCourt(id));
        courtCache.evict(id);
    }

    private Court managedCourt(UUID id) {
        var court = courts.findById(id).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "COURT_NOT_FOUND", "Court not found"));
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var admin = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && !court.getVenue().getOwner().getId().toString().equals(authentication.getName())) {
            throw new AccessDeniedException("Only the venue owner or an administrator can manage this court");
        }
        return court;
    }
}
