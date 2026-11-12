package com.badmintonhub.backend.dto.request;

import com.badmintonhub.backend.validation.ValidTimeZone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Full input for creating or replacing a venue; ownership comes from the authenticated account. */
public record VenueRequest(
    @NotBlank @Size(max = 150) String name,
    @NotBlank @Size(max = 255) String address,
    @NotBlank @Size(max = 64) @ValidTimeZone String timeZone
) {
}
