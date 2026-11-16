package com.badmintonhub.backend.service;

import java.util.UUID;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.badmintonhub.backend.dto.request.CreateCourtRequest;
import com.badmintonhub.backend.dto.request.UpdateCourtRequest;
import com.badmintonhub.backend.dto.response.CourtResponse;
import com.badmintonhub.backend.entity.Court;
import com.badmintonhub.backend.entity.Venue;
import com.badmintonhub.backend.exception.ApiException;
import com.badmintonhub.backend.repository.CourtRepository;
import com.badmintonhub.backend.repository.VenueRepository;
import com.badmintonhub.backend.config.CacheConfig;

@Service
@Validated
public class CourtService {
    private final CourtRepository courts;
    private final VenueRepository venues;
    private final Cache courtCache;

    public CourtService(CourtRepository courtRepository, VenueRepository venueRepository, CacheManager cacheManager) {
        this.courts = courtRepository;
        this.venues = venueRepository;
        this.courtCache = java.util.Objects.requireNonNull(cacheManager.getCache(CacheConfig.COURT_DETAILS),
            "Court details cache must be configured");
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Transactional
    public CourtResponse createCourt(@NotNull @Valid CreateCourtRequest request) {
        var venue = venues.findById(request.venueId()).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "VENUE_NOT_FOUND", "Venue not found"));
        checkVenueOwnership(venue);

        var court = new Court();
        court.setVenue(venue);
        court.setName(request.name().strip());
        court.setBaseHourlyRateVnd(request.baseHourlyRateVnd());
        court.setActive(true);

        var saved = courts.save(court);
        return CourtResponse.from(saved);
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

    @Transactional(readOnly = true)
    public Page<CourtResponse> getCourtsByVenue(@NotNull UUID venueId, Pageable pageable) {
        return courts.findByVenue_Id(venueId, pageable).map(CourtResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<CourtResponse> getActiveCourtsByVenue(@NotNull UUID venueId, Pageable pageable) {
        return courts.findByVenue_IdAndActiveTrue(venueId, pageable).map(CourtResponse::from);
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

    private void checkVenueOwnership(Venue venue) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var admin = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && !venue.getOwner().getId().toString().equals(authentication.getName())) {
            throw new AccessDeniedException("Only the venue owner or an administrator can manage courts in this venue");
        }
    }

    private Court managedCourt(UUID id) {
        var court = courts.findById(id).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "COURT_NOT_FOUND", "Court not found"));
        checkVenueOwnership(court.getVenue());
        return court;
    }
}
