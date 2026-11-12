package com.badmintonhub.backend.dto;

import com.badmintonhub.backend.dto.request.VenueRequest;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class VenueRequestTests extends DtoValidationSupport {
    @Test
    void acceptsRealZoneAndRejectsUnknownZone() {
        assertTrue(invalidFields(new VenueRequest("San A", "Ha Noi", "Asia/Ho_Chi_Minh")).isEmpty());
        assertTrue(invalidFields(new VenueRequest("San A", "Ha Noi", "UTC")).isEmpty());
        assertEquals(Set.of("timeZone"), invalidFields(new VenueRequest("San A", "Ha Noi", "Asia/Unknown")));
    }

    @Test
    void requiresFieldsAndRespectsColumnLengths() {
        assertEquals(Set.of("name", "address", "timeZone"), invalidFields(new VenueRequest(" ", null, null)));
        assertEquals(Set.of("name", "address"), invalidFields(
            new VenueRequest("n".repeat(151), "a".repeat(256), "UTC")));
    }
}
