package com.badmintonhub.backend.dto;

import com.badmintonhub.backend.dto.request.PricingRuleRequest;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PricingRuleRequestTests extends DtoValidationSupport {
    private PricingRuleRequest rule(int day, LocalTime end, LocalDate until) {
        return new PricingRuleRequest(UUID.randomUUID(), day, LocalTime.of(8, 0), end,
            0L, LocalDate.of(2026, 11, 12), until);
    }

    @Test
    void permitsOpenEndedRuleAndRequiresStrictlyIncreasingRanges() {
        assertTrue(invalidFields(rule(1, LocalTime.of(9, 0), null)).isEmpty());
        assertEquals(Set.of("timeRangeValid"), invalidFields(rule(7, LocalTime.of(8, 0), null)));
        assertEquals(Set.of("dateRangeValid"), invalidFields(
            rule(1, LocalTime.of(9, 0), LocalDate.of(2026, 11, 12))));
        assertEquals(Set.of("timeRangeValid", "dateRangeValid"), invalidFields(
            rule(1, LocalTime.of(7, 0), LocalDate.of(2026, 11, 11))));
    }

    @Test
    void rejectsInvalidDayMissingValuesAndNegativeRate() {
        assertEquals(Set.of("dayOfWeek"), invalidFields(rule(0, LocalTime.of(9, 0), null)));
        assertEquals(Set.of("dayOfWeek"), invalidFields(rule(8, LocalTime.of(9, 0), null)));
        assertEquals(Set.of("courtId", "dayOfWeek", "startLocalTime", "endLocalTime", "hourlyRateVnd", "validFrom"),
            invalidFields(new PricingRuleRequest(null, null, null, null, null, null, null)));
        assertTrue(invalidFields(new PricingRuleRequest(UUID.randomUUID(), 1, LocalTime.of(8, 0),
            LocalTime.of(9, 0), -1L, LocalDate.of(2026, 11, 12), null)).contains("hourlyRateVnd"));
    }
}
