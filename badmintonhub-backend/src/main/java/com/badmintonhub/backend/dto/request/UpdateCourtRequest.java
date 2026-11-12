package com.badmintonhub.backend.dto.request;

import jakarta.validation.constraints.*;

/** Full replacement of editable court fields; the venue cannot be reassigned. */
public record UpdateCourtRequest(
    @NotBlank @Size(max = 100) String name,
    @NotNull @PositiveOrZero Long baseHourlyRateVnd,
    @NotNull Boolean active
) {
}
