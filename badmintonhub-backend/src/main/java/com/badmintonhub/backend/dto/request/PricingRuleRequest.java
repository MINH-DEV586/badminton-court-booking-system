package com.badmintonhub.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** validUntil is exclusive. Null means the rule has no end date. */
public record PricingRuleRequest(
    @NotNull UUID courtId,
    @NotNull @Min(1) @Max(7) Integer dayOfWeek,
    @NotNull LocalTime startLocalTime,
    @NotNull LocalTime endLocalTime,
    @NotNull @PositiveOrZero Long hourlyRateVnd,
    @NotNull LocalDate validFrom,
    LocalDate validUntil
) {
    @AssertTrue(message = "endLocalTime must be after startLocalTime")
    @JsonIgnore
    public boolean isTimeRangeValid() {
        return startLocalTime == null || endLocalTime == null || startLocalTime.isBefore(endLocalTime);
    }

    @AssertTrue(message = "validUntil must be after validFrom")
    @JsonIgnore
    public boolean isDateRangeValid() {
        return validFrom == null || validUntil == null || validFrom.isBefore(validUntil);
    }
}
