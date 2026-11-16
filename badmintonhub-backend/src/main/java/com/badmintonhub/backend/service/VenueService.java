package com.badmintonhub.backend.service;

import com.badmintonhub.backend.dto.request.VenueRequest;
import com.badmintonhub.backend.dto.response.VenueResponse;
import com.badmintonhub.backend.entity.Venue;
import com.badmintonhub.backend.exception.ApiException;
import com.badmintonhub.backend.repository.UserAccountRepository;
import com.badmintonhub.backend.repository.VenueRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Service
@Validated
public class VenueService {
    private final VenueRepository venues;
    private final UserAccountRepository users;

    public VenueService(VenueRepository venues, UserAccountRepository users) {
        this.venues = venues;
        this.users = users;
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Transactional
    public VenueResponse createVenue(@NotNull @Valid VenueRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UUID ownerId = UUID.fromString(auth.getName());
        var owner = users.findById(ownerId)
            .filter(user -> user.isActive())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authenticated user not found"));

        var venue = new Venue();
        venue.setOwner(owner);
        venue.setName(request.name().strip());
        venue.setAddress(request.address().strip());
        venue.setTimeZone(request.timeZone().strip());

        return VenueResponse.from(venues.save(venue));
    }

    @Transactional(readOnly = true)
    public VenueResponse getVenue(@NotNull UUID id) {
        var venue = venues.findById(id).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "VENUE_NOT_FOUND", "Venue not found"));
        return VenueResponse.from(venue);
    }

    @Transactional(readOnly = true)
    public Page<VenueResponse> getAllVenues(Pageable pageable) {
        return venues.findAll(pageable).map(VenueResponse::from);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Transactional(readOnly = true)
    public Page<VenueResponse> getOwnerVenues(@NotNull UUID ownerId, Pageable pageable) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var admin = auth.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && !ownerId.toString().equals(auth.getName())) {
            throw new AccessDeniedException("Cannot view venues belonging to another owner");
        }
        return venues.findByOwner_Id(ownerId, pageable).map(VenueResponse::from);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Transactional
    public VenueResponse updateVenue(@NotNull UUID id, @NotNull @Valid VenueRequest request) {
        var venue = managedVenue(id);
        venue.setName(request.name().strip());
        venue.setAddress(request.address().strip());
        venue.setTimeZone(request.timeZone().strip());
        return VenueResponse.from(venue);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Transactional
    public void deleteVenue(@NotNull UUID id) {
        var venue = managedVenue(id);
        venues.delete(venue);
    }

    private Venue managedVenue(UUID id) {
        var venue = venues.findById(id).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "VENUE_NOT_FOUND", "Venue not found"));
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var admin = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && !venue.getOwner().getId().toString().equals(authentication.getName())) {
            throw new AccessDeniedException("Only the venue owner or an administrator can manage this venue");
        }
        return venue;
    }
}
