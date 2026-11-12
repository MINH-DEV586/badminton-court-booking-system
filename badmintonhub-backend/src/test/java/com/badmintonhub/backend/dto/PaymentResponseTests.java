package com.badmintonhub.backend.dto;

import com.badmintonhub.backend.dto.response.PaymentResponse;
import com.badmintonhub.backend.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PaymentResponseTests {
    @Test
    void mapsAllFieldsAndSerializesFlatResponse() {
        var id = UUID.randomUUID();
        var bookingId = UUID.randomUUID();
        var booking = new Booking();
        ReflectionTestUtils.setField(booking, "id", bookingId);
        var entity = new Payment();
        ReflectionTestUtils.setField(entity, "id", id);
        ReflectionTestUtils.setField(entity, "provider", "VNPAY");
        ReflectionTestUtils.setField(entity, "providerPaymentId", "provider-reference-1");
        ReflectionTestUtils.setField(entity, "amountVnd", 120000L);
        ReflectionTestUtils.setField(entity, "status", Payment.Status.SUCCEEDED);
        ReflectionTestUtils.setField(entity, "paidAt", Instant.parse("2026-11-12T08:00:00Z"));
        ReflectionTestUtils.setField(entity, "createdAt", Instant.parse("2026-11-11T08:00:00Z"));
        ReflectionTestUtils.setField(entity, "booking", booking);
        var response = PaymentResponse.from(entity);
        assertEquals(new PaymentResponse(id, bookingId, "VNPAY", "provider-reference-1", 120000L, Payment.Status.SUCCEEDED, Instant.parse("2026-11-12T08:00:00Z"), Instant.parse("2026-11-11T08:00:00Z")), response);

        var json = JsonMapper.builder().build().valueToTree(response);
        assertEquals(8, json.size());
        assertTrue(json.has("id"));
        assertTrue(json.has("bookingId"));
        assertTrue(json.has("provider"));
        assertTrue(json.has("providerPaymentId"));
        assertTrue(json.has("amountVnd"));
        assertTrue(json.has("status"));
        assertTrue(json.has("paidAt"));
        assertTrue(json.has("createdAt"));
        assertEquals(bookingId.toString(), json.get("bookingId").asText());
        assertFalse(json.has("booking"));
        assertFalse(json.toString().contains("private-password-hash"));
    }
}
