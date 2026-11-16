package com.badmintonhub.backend.controller;

import com.badmintonhub.backend.dto.request.*;
import com.badmintonhub.backend.dto.response.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.*;

import com.badmintonhub.backend.service.CourtBlockService;

@RestController
@RequestMapping("/api")
public class CourtBlockController {
    private final CourtBlockService blocks;
    public CourtBlockController(CourtBlockService blocks) { this.blocks = blocks; }

    @GetMapping("/court-blocks/{id}")
    public CourtBlockResponse getBlock(@PathVariable UUID id) { return blocks.getBlock(id); }

    @GetMapping("/courts/{courtId}/blocks")
    public PagedModel<CourtBlockResponse> getActiveBlocks(@PathVariable UUID courtId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return new PagedModel<>(blocks.getActiveBlocksForCourt(courtId,
            PageRequest.of(page, size, Sort.by("startsAt").ascending())));
    }

    @PostMapping("/owner/court-blocks")
    public ResponseEntity<CourtBlockResponse> createBlock(@Valid @RequestBody CreateCourtBlockRequest request) {
        var response = blocks.createBlock(request);
        return ResponseEntity.created(URI.create("/api/court-blocks/" + response.id())).body(response);
    }

    @PostMapping("/owner/court-blocks/{id}/cancel")
    public CourtBlockResponse cancelBlock(@PathVariable UUID id) { return blocks.cancelBlock(id); }
}
