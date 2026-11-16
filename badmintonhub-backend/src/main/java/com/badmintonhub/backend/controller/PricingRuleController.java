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

import com.badmintonhub.backend.service.PricingRuleService;

@RestController
@RequestMapping("/api")
public class PricingRuleController {
    private final PricingRuleService pricing;
    public PricingRuleController(PricingRuleService pricing) { this.pricing = pricing; }

    @GetMapping("/pricing-rules/{id}")
    public PricingRuleResponse getRule(@PathVariable UUID id) { return pricing.getRule(id); }

    @GetMapping("/courts/{courtId}/pricing-rules")
    public List<PricingRuleResponse> getRules(@PathVariable UUID courtId,
        @RequestParam(required = false) @Min(1) @Max(7) Integer dayOfWeek) {
        return dayOfWeek == null ? pricing.getRulesForCourt(courtId)
            : pricing.getRulesForCourtAndDay(courtId, dayOfWeek);
    }

    @PostMapping("/owner/pricing-rules")
    public ResponseEntity<PricingRuleResponse> createRule(@Valid @RequestBody PricingRuleRequest request) {
        var response = pricing.createRule(request);
        return ResponseEntity.created(URI.create("/api/pricing-rules/" + response.id())).body(response);
    }

    @DeleteMapping("/owner/pricing-rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        pricing.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
