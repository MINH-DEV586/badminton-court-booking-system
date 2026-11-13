package com.badmintonhub.backend.service;

import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
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

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @CacheEvict(cacheNames = CacheConfig.COURT_DETAILS, key = "#p0")
    @Transactional
    public CourtResponse updateCourt(@NotNull UUID id, @NotNull @Valid UpdateCourtRequest request) {
        var court = managedCourt(id);
        court.setName(request.name().strip());
        court.setBaseHourlyRateVnd(request.baseHourlyRateVnd());
        court.setActive(request.active());
        return CourtResponse.from(court);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @CacheEvict(cacheNames = CacheConfig.COURT_DETAILS, key = "#p0")
    @Transactional
    public void deleteCourt(@NotNull UUID id) {
        courts.delete(managedCourt(id));
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
