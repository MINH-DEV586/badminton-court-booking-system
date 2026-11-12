package com.badmintonhub.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

/** Supports blocking a court immediately or scheduling a block. The creator is server assigned. */
public record CreateCourtBlockRequest(
    @NotNull UUID courtId,
    @NotNull Instant startsAt,
    @NotNull @Future Instant endsAt,
    @NotBlank @Size(max = 255) String reason
) {
    @AssertTrue(message = "endsAt must be after startsAt")
    @JsonIgnore
    public boolean isTimeRangeValid() {
        return startsAt == null || endsAt == null || startsAt.isBefore(endsAt);
    }
}
