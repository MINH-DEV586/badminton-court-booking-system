package com.badmintonhub.backend.controller;

import com.badmintonhub.backend.dto.request.VenueRequest;
import com.badmintonhub.backend.dto.response.VenueResponse;
import com.badmintonhub.backend.service.VenueService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class VenueController {
    private final VenueService venues;

    public VenueController(VenueService venues) { this.venues = venues; }

    @GetMapping("/venues")
    public PagedModel<VenueResponse> getVenues(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return new PagedModel<>(venues.getAllVenues(PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/venues/{id}")
    public VenueResponse getVenue(@PathVariable UUID id) { return venues.getVenue(id); }

    @GetMapping("/owner/venues")
    public PagedModel<VenueResponse> getMyVenues(@AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return new PagedModel<>(venues.getOwnerVenues(UUID.fromString(jwt.getSubject()),
            PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/admin/users/{ownerId}/venues")
    public PagedModel<VenueResponse> getOwnerVenues(@PathVariable UUID ownerId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return new PagedModel<>(venues.getOwnerVenues(ownerId,
            PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @PostMapping("/owner/venues")
    public ResponseEntity<VenueResponse> createVenue(@Valid @RequestBody VenueRequest request) {
        var response = venues.createVenue(request);
        return ResponseEntity.created(URI.create("/api/venues/" + response.id())).body(response);
    }

    @PutMapping("/owner/venues/{id}")
    public VenueResponse updateVenue(@PathVariable UUID id, @Valid @RequestBody VenueRequest request) {
        return venues.updateVenue(id, request);
    }

    @DeleteMapping("/owner/venues/{id}")
    public ResponseEntity<Void> deleteVenue(@PathVariable UUID id) {
        venues.deleteVenue(id);
        return ResponseEntity.noContent().build();
    }
}
