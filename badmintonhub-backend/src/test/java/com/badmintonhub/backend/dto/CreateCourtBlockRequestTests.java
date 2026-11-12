package com.badmintonhub.backend.dto;

import com.badmintonhub.backend.dto.request.CreateCourtBlockRequest;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CreateCourtBlockRequestTests extends DtoValidationSupport {
    @Test
    void supportsOngoingBlockAndRejectsExpiredOrReversedRanges() {
        var now = Instant.parse("2026-11-12T00:00:00Z");
        assertTrue(invalidFields(new CreateCourtBlockRequest(UUID.randomUUID(), now.minusSeconds(60),
            now.plusSeconds(3600), "Maintenance")).isEmpty());
        assertEquals(Set.of("timeRangeValid"), invalidFields(new CreateCourtBlockRequest(UUID.randomUUID(),
            now.plusSeconds(7200), now.plusSeconds(3600), "Maintenance")));
        assertEquals(Set.of("endsAt"), invalidFields(new CreateCourtBlockRequest(UUID.randomUUID(),
            now.minusSeconds(7200), now.minusSeconds(3600), "Maintenance")));
        assertEquals(Set.of("courtId", "startsAt", "endsAt", "reason"),
            invalidFields(new CreateCourtBlockRequest(null, null, null, " ")));
        assertEquals(Set.of("reason"), invalidFields(new CreateCourtBlockRequest(UUID.randomUUID(),
            now, now.plusSeconds(3600), "r".repeat(256))));
    }
}
