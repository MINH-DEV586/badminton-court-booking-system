package com.badmintonhub.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

/** Customer, amount, status and payment hold expiry are determined by the server. */
public record CreateBookingRequest(
    @NotNull UUID courtId,
    @NotNull @Future Instant startsAt,
    @NotNull Instant endsAt
) {
    @AssertTrue(message = "endsAt must be after startsAt")
    @JsonIgnore
    public boolean isTimeRangeValid() {
        return startsAt == null || endsAt == null || startsAt.isBefore(endsAt);
    }
}
