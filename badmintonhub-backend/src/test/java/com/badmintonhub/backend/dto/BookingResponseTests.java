package com.badmintonhub.backend.dto;

import com.badmintonhub.backend.dto.response.BookingResponse;
import com.badmintonhub.backend.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class BookingResponseTests {
    @Test
    void mapsAllFieldsAndSerializesFlatResponse() {
        var id = UUID.randomUUID();
        var customerId = UUID.randomUUID();
        var customer = new UserAccount();
        ReflectionTestUtils.setField(customer, "id", customerId);
        customer.setPasswordHash("private-password-hash");
        var courtId = UUID.randomUUID();
        var court = new Court();
        ReflectionTestUtils.setField(court, "id", courtId);
        var entity = new Booking();
        ReflectionTestUtils.setField(entity, "id", id);
        ReflectionTestUtils.setField(entity, "startsAt", Instant.parse("2026-11-12T08:00:00Z"));
        ReflectionTestUtils.setField(entity, "endsAt", Instant.parse("2026-11-12T09:00:00Z"));
        ReflectionTestUtils.setField(entity, "status", Booking.Status.CONFIRMED);
        ReflectionTestUtils.setField(entity, "totalAmountVnd", 120000L);
        ReflectionTestUtils.setField(entity, "holdExpiresAt", null);
        ReflectionTestUtils.setField(entity, "createdAt", Instant.parse("2026-11-11T08:00:00Z"));
        ReflectionTestUtils.setField(entity, "customer", customer);
        ReflectionTestUtils.setField(entity, "court", court);
        var response = BookingResponse.from(entity);
        assertEquals(new BookingResponse(id, customerId, courtId, Instant.parse("2026-11-12T08:00:00Z"), Instant.parse("2026-11-12T09:00:00Z"), Booking.Status.CONFIRMED, 120000L, null, Instant.parse("2026-11-11T08:00:00Z")), response);

        var json = JsonMapper.builder().build().valueToTree(response);
        assertEquals(9, json.size());
        assertTrue(json.has("id"));
        assertTrue(json.has("customerId"));
        assertTrue(json.has("courtId"));
        assertTrue(json.has("startsAt"));
        assertTrue(json.has("endsAt"));
        assertTrue(json.has("status"));
        assertTrue(json.has("totalAmountVnd"));
        assertTrue(json.has("holdExpiresAt"));
        assertTrue(json.has("createdAt"));
        assertEquals(customerId.toString(), json.get("customerId").asText());
        assertFalse(json.has("customer"));
        assertEquals(courtId.toString(), json.get("courtId").asText());
        assertFalse(json.has("court"));
        assertFalse(json.toString().contains("private-password-hash"));
    }
}
