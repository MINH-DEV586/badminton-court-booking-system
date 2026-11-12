package com.badmintonhub.backend.dto;

import com.badmintonhub.backend.dto.request.UpdateCourtRequest;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class UpdateCourtRequestTests extends DtoValidationSupport {
    @Test
    void supportsDeactivatingCourtAndRejectsMissingFields() {
        assertTrue(invalidFields(new UpdateCourtRequest("Court 1", 0L, false)).isEmpty());
        assertEquals(Set.of("name", "baseHourlyRateVnd", "active"),
            invalidFields(new UpdateCourtRequest(null, null, null)));
        assertEquals(Set.of("name", "baseHourlyRateVnd"),
            invalidFields(new UpdateCourtRequest("a".repeat(101), -1L, true)));
    }
}
