package com.badmintonhub.backend.dto;

import com.badmintonhub.backend.dto.response.CourtResponse;
import com.badmintonhub.backend.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class CourtResponseTests {
    @Test
    void mapsAllFieldsAndSerializesFlatResponse() {
        var id = UUID.randomUUID();
        var venueId = UUID.randomUUID();
        var venue = new Venue();
        ReflectionTestUtils.setField(venue, "id", venueId);
        var entity = new Court();
        ReflectionTestUtils.setField(entity, "id", id);
        ReflectionTestUtils.setField(entity, "name", "Court 1");
        ReflectionTestUtils.setField(entity, "baseHourlyRateVnd", 120000L);
        ReflectionTestUtils.setField(entity, "active", false);
        ReflectionTestUtils.setField(entity, "createdAt", Instant.parse("2026-11-12T08:00:00Z"));
        ReflectionTestUtils.setField(entity, "venue", venue);
        var response = CourtResponse.from(entity);
        assertEquals(new CourtResponse(id, venueId, "Court 1", 120000L, false, Instant.parse("2026-11-12T08:00:00Z")), response);

        var json = JsonMapper.builder().build().valueToTree(response);
        assertEquals(6, json.size());
        assertTrue(json.has("id"));
        assertTrue(json.has("venueId"));
        assertTrue(json.has("name"));
        assertTrue(json.has("baseHourlyRateVnd"));
        assertTrue(json.has("active"));
        assertTrue(json.has("createdAt"));
        assertEquals(venueId.toString(), json.get("venueId").asText());
        assertFalse(json.has("venue"));
        assertFalse(json.toString().contains("private-password-hash"));
    }
}
