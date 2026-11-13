package com.badmintonhub.backend.controller;

import com.badmintonhub.backend.dto.request.UpdateCourtRequest;
import com.badmintonhub.backend.dto.response.CourtResponse;
import com.badmintonhub.backend.service.CourtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CourtController {
    private final CourtService courts;

    public CourtController(CourtService courts) {
        this.courts = courts;
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
