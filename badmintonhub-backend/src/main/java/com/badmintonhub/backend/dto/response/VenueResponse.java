package com.badmintonhub.backend.dto.response;

import com.badmintonhub.backend.entity.Venue;
import java.time.*;
import java.util.UUID;

/** Map within the service transaction while required entity relationships are available. */
public record VenueResponse(
    UUID id,
    UUID ownerId,
    String name,
    String address,
    String timeZone,
    Instant createdAt
) {
    public static VenueResponse from(Venue entity) {
        return new VenueResponse(
            entity.getId(),
            entity.getOwner().getId(),
            entity.getName(),
            entity.getAddress(),
            entity.getTimeZone(),
            entity.getCreatedAt());
    }
}
