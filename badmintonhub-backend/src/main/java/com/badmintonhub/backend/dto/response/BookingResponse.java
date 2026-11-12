package com.badmintonhub.backend.dto.response;

import com.badmintonhub.backend.entity.Booking;
import java.time.*;
import java.util.UUID;

/** Map within the service transaction while required entity relationships are available. */
public record BookingResponse(
    UUID id,
    UUID customerId,
    UUID courtId,
    Instant startsAt,
    Instant endsAt,
    Booking.Status status,
    long totalAmountVnd,
    Instant holdExpiresAt,
    Instant createdAt
) {
    public static BookingResponse from(Booking entity) {
        return new BookingResponse(
            entity.getId(),
            entity.getCustomer().getId(),
            entity.getCourt().getId(),
            entity.getStartsAt(),
            entity.getEndsAt(),
            entity.getStatus(),
            entity.getTotalAmountVnd(),
            entity.getHoldExpiresAt(),
            entity.getCreatedAt());
    }
}
