package com.badmintonhub.backend.dto.response;

import com.badmintonhub.backend.entity.CourtBlock;
import java.time.*;
import java.util.UUID;

/** Map within the service transaction while required entity relationships are available. */
public record CourtBlockResponse(
    UUID id,
    UUID courtId,
    UUID createdById,
    Instant startsAt,
    Instant endsAt,
    String reason,
    Instant createdAt,
    Instant cancelledAt
) {
    public static CourtBlockResponse from(CourtBlock entity) {
        return new CourtBlockResponse(
            entity.getId(),
            entity.getCourt().getId(),
            entity.getCreatedBy().getId(),
            entity.getStartsAt(),
            entity.getEndsAt(),
            entity.getReason(),
            entity.getCreatedAt(),
            entity.getCancelledAt());
    }
}
