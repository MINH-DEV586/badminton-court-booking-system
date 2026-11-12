package com.badmintonhub.backend.dto;

import com.badmintonhub.backend.dto.request.CreateBookingRequest;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CreateBookingRequestTests extends DtoValidationSupport {
    @Test
    void requiresFutureStartAndStrictlyIncreasingRange() {
        var future = Instant.parse("2026-11-13T08:00:00Z");
        assertTrue(invalidFields(new CreateBookingRequest(UUID.randomUUID(), future, future.plusSeconds(3600))).isEmpty());
        assertEquals(Set.of("timeRangeValid"),
            invalidFields(new CreateBookingRequest(UUID.randomUUID(), future, future)));
        assertEquals(Set.of("timeRangeValid"),
            invalidFields(new CreateBookingRequest(UUID.randomUUID(), future, future.minusSeconds(1))));
        assertEquals(Set.of("startsAt"), invalidFields(new CreateBookingRequest(UUID.randomUUID(),
            Instant.parse("2026-11-11T08:00:00Z"), future)));
        assertEquals(Set.of("courtId", "startsAt", "endsAt"),
            invalidFields(new CreateBookingRequest(null, null, null)));
    }
}
