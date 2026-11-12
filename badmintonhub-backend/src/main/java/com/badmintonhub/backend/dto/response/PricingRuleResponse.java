package com.badmintonhub.backend.dto.response;

import com.badmintonhub.backend.entity.PricingRule;
import java.time.*;
import java.util.UUID;

/** Map within the service transaction while required entity relationships are available. */
public record PricingRuleResponse(
    UUID id,
    UUID courtId,
    int dayOfWeek,
    LocalTime startLocalTime,
    LocalTime endLocalTime,
    long hourlyRateVnd,
    LocalDate validFrom,
    LocalDate validUntil
) {
    public static PricingRuleResponse from(PricingRule entity) {
        return new PricingRuleResponse(
            entity.getId(),
            entity.getCourt().getId(),
            entity.getDayOfWeek(),
            entity.getStartLocalTime(),
            entity.getEndLocalTime(),
            entity.getHourlyRateVnd(),
            entity.getValidFrom(),
            entity.getValidUntil());
    }
}
