package com.badmintonhub.backend.dto;

import com.badmintonhub.backend.dto.request.CreatePaymentRequest;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CreatePaymentRequestTests extends DtoValidationSupport {
    @Test
    void requiresBookingAndProviderWithinDatabaseLimit() {
        assertTrue(invalidFields(new CreatePaymentRequest(UUID.randomUUID(), "VNPAY")).isEmpty());
        assertEquals(Set.of("bookingId", "provider"), invalidFields(new CreatePaymentRequest(null, " ")));
        assertEquals(Set.of("provider"),
            invalidFields(new CreatePaymentRequest(UUID.randomUUID(), "a".repeat(33))));
    }
}
