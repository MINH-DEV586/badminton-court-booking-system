package com.badmintonhub.backend.controller;

import com.badmintonhub.backend.dto.request.UpdateCourtRequest;
import com.badmintonhub.backend.dto.request.CreateCourtRequest;
import com.badmintonhub.backend.dto.response.CourtResponse;
import com.badmintonhub.backend.service.CourtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.net.URI;

@RestController
@RequestMapping("/api")
public class CourtController {
    private final CourtService courts;

    public CourtController(CourtService courts) {
        this.courts = courts;
    }

    @PostMapping("/owner/courts")
    public ResponseEntity<CourtResponse> createCourt(@Valid @RequestBody CreateCourtRequest request) {
        var response = courts.createCourt(request);
        return ResponseEntity.created(URI.create("/api/courts/" + response.id())).body(response);
    }

    @GetMapping("/venues/{venueId}/courts")
    public PagedModel<CourtResponse> getCourts(@PathVariable UUID venueId,
        @RequestParam(defaultValue = "true") boolean activeOnly,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return new PagedModel<>(activeOnly ? courts.getActiveCourtsByVenue(venueId, pageable)
            : courts.getCourtsByVenue(venueId, pageable));
    }

    @GetMapping("/courts/{id}")
    public CourtResponse getCourt(@PathVariable UUID id) {
        return courts.getCourt(id);
    }

    @PutMapping("/owner/courts/{id}")
    public CourtResponse updateCourt(@PathVariable UUID id, @Valid @RequestBody UpdateCourtRequest request) {
        return courts.updateCourt(id, request);
    }

    @DeleteMapping("/owner/courts/{id}")
    public ResponseEntity<Void> deleteCourt(@PathVariable UUID id) {
        courts.deleteCourt(id);
        return ResponseEntity.noContent().build();
    }
}
