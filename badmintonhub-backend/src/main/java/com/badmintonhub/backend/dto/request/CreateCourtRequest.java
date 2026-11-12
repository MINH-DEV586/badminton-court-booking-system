package com.badmintonhub.backend.dto.request;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record CreateCourtRequest(
    @NotNull UUID venueId,
    @NotBlank @Size(max = 100) String name,
    @NotNull @PositiveOrZero Long baseHourlyRateVnd
) {
}
