package com.badmintonhub.backend.dto;

import com.badmintonhub.backend.dto.request.CreateCourtRequest;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CreateCourtRequestTests extends DtoValidationSupport {
    @Test
    void allowsFreeCourtButRequiresExplicitRate() {
        assertTrue(invalidFields(new CreateCourtRequest(UUID.randomUUID(), "Court 1", 0L)).isEmpty());
        assertEquals(Set.of("venueId", "name", "baseHourlyRateVnd"),
            invalidFields(new CreateCourtRequest(null, " ", null)));
        assertEquals(Set.of("name", "baseHourlyRateVnd"),
            invalidFields(new CreateCourtRequest(UUID.randomUUID(), "a".repeat(101), -1L)));
    }
}
