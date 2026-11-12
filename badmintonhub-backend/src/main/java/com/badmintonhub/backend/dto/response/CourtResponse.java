package com.badmintonhub.backend.dto.response;

import com.badmintonhub.backend.entity.Court;
import java.time.*;
import java.util.UUID;

/** Map within the service transaction while required entity relationships are available. */
public record CourtResponse(
    UUID id,
    UUID venueId,
    String name,
    long baseHourlyRateVnd,
    boolean active,
    Instant createdAt
) {
    public static CourtResponse from(Court entity) {
        return new CourtResponse(
            entity.getId(),
            entity.getVenue().getId(),
            entity.getName(),
            entity.getBaseHourlyRateVnd(),
            entity.isActive(),
            entity.getCreatedAt());
    }
}
