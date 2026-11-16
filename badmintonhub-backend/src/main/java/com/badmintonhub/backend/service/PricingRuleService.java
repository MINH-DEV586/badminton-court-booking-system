package com.badmintonhub.backend.service;

import com.badmintonhub.backend.dto.request.PricingRuleRequest;
import com.badmintonhub.backend.dto.response.PricingRuleResponse;
import com.badmintonhub.backend.entity.Court;
import com.badmintonhub.backend.entity.PricingRule;
import com.badmintonhub.backend.exception.ApiException;
import com.badmintonhub.backend.repository.CourtRepository;
import com.badmintonhub.backend.repository.PricingRuleRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.*;
import java.util.List;
import java.util.UUID;

@Service
@Validated
public class PricingRuleService {
    private final PricingRuleRepository pricingRules;
    private final CourtRepository courts;

    public PricingRuleService(PricingRuleRepository pricingRules, CourtRepository courts) {
        this.pricingRules = pricingRules;
        this.courts = courts;
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Transactional
    public PricingRuleResponse createRule(@NotNull @Valid PricingRuleRequest request) {
        var court = courts.findById(request.courtId()).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "COURT_NOT_FOUND", "Court not found"));
        checkCourtOwnership(court);

        var rule = new PricingRule();
        rule.setCourt(court);
        rule.setDayOfWeek(request.dayOfWeek());
        rule.setStartLocalTime(request.startLocalTime());
        rule.setEndLocalTime(request.endLocalTime());
        rule.setHourlyRateVnd(request.hourlyRateVnd());
        rule.setValidFrom(request.validFrom());
        rule.setValidUntil(request.validUntil());

        return PricingRuleResponse.from(pricingRules.save(rule));
    }

    @Transactional(readOnly = true)
    public PricingRuleResponse getRule(@NotNull UUID id) {
        var rule = pricingRules.findById(id).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "PRICING_RULE_NOT_FOUND", "Pricing rule not found"));
        return PricingRuleResponse.from(rule);
    }

    @Transactional(readOnly = true)
    public List<PricingRuleResponse> getRulesForCourtAndDay(@NotNull UUID courtId, int dayOfWeek) {
        return pricingRules.findByCourt_IdAndDayOfWeekOrderByStartLocalTimeAsc(courtId, dayOfWeek)
            .stream()
            .map(PricingRuleResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PricingRuleResponse> getRulesForCourt(@NotNull UUID courtId) {
        return pricingRules.findByCourt_Id(courtId)
            .stream()
            .map(PricingRuleResponse::from)
            .toList();
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Transactional
    public void deleteRule(@NotNull UUID id) {
        var rule = pricingRules.findById(id).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "PRICING_RULE_NOT_FOUND", "Pricing rule not found"));
        checkCourtOwnership(rule.getCourt());
        pricingRules.delete(rule);
    }

    @Transactional(readOnly = true)
    public long calculateTotalPrice(@NotNull Court court, @NotNull Instant startsAt, @NotNull Instant endsAt) {
        if (!startsAt.isBefore(endsAt)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TIME_RANGE", "startsAt must be before endsAt");
        }

        ZoneId zoneId = ZoneId.of(court.getVenue().getTimeZone());
        ZonedDateTime startZoned = startsAt.atZone(zoneId);
        ZonedDateTime endZoned = endsAt.atZone(zoneId);

        // Split interval into 15-minute segments for pricing calculation
        long totalVnd = 0;
        final long segmentMinutes = 15;
        ZonedDateTime current = startZoned;

        while (current.isBefore(endZoned)) {
            ZonedDateTime next = current.plusMinutes(segmentMinutes);
            if (next.isAfter(endZoned)) {
                next = endZoned;
            }

            long durationSeconds = Duration.between(current, next).toSeconds();
            long hourlyRate = resolveHourlyRate(court, current.toLocalDate(), current.toLocalTime(), current.getDayOfWeek().getValue());

            long segmentCost = (hourlyRate * durationSeconds) / 3600;
            totalVnd += segmentCost;

            current = next;
        }

        return totalVnd;
    }

    private long resolveHourlyRate(Court court, LocalDate localDate, LocalTime localTime, int dayOfWeek) {
        var rules = pricingRules.findByCourt_IdAndDayOfWeekOrderByStartLocalTimeAsc(court.getId(), dayOfWeek);

        for (var rule : rules) {
            boolean dateValid = !localDate.isBefore(rule.getValidFrom()) &&
                (rule.getValidUntil() == null || localDate.isBefore(rule.getValidUntil()));

            boolean timeValid = !localTime.isBefore(rule.getStartLocalTime()) &&
                localTime.isBefore(rule.getEndLocalTime());

            if (dateValid && timeValid) {
                return rule.getHourlyRateVnd();
            }
        }

        return court.getBaseHourlyRateVnd();
    }

    private void checkCourtOwnership(Court court) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var admin = auth.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && !court.getVenue().getOwner().getId().toString().equals(auth.getName())) {
            throw new AccessDeniedException("Only the venue owner or an administrator can manage pricing rules for this court");
        }
    }
}
