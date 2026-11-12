package com.badmintonhub.backend.dto;

import com.badmintonhub.backend.dto.response.VenueResponse;
import com.badmintonhub.backend.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class VenueResponseTests {
    @Test
    void mapsAllFieldsAndSerializesFlatResponse() {
        var id = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        var owner = new UserAccount();
        ReflectionTestUtils.setField(owner, "id", ownerId);
        owner.setPasswordHash("private-password-hash");
        var entity = new Venue();
        ReflectionTestUtils.setField(entity, "id", id);
        ReflectionTestUtils.setField(entity, "name", "Venue A");
        ReflectionTestUtils.setField(entity, "address", "Ha Noi");
        ReflectionTestUtils.setField(entity, "timeZone", "Asia/Ho_Chi_Minh");
        ReflectionTestUtils.setField(entity, "createdAt", Instant.parse("2026-11-12T08:00:00Z"));
        ReflectionTestUtils.setField(entity, "owner", owner);
        var response = VenueResponse.from(entity);
        assertEquals(new VenueResponse(id, ownerId, "Venue A", "Ha Noi", "Asia/Ho_Chi_Minh", Instant.parse("2026-11-12T08:00:00Z")), response);

        var json = JsonMapper.builder().build().valueToTree(response);
        assertEquals(6, json.size());
        assertTrue(json.has("id"));
        assertTrue(json.has("ownerId"));
        assertTrue(json.has("name"));
        assertTrue(json.has("address"));
        assertTrue(json.has("timeZone"));
        assertTrue(json.has("createdAt"));
        assertEquals(ownerId.toString(), json.get("ownerId").asText());
        assertFalse(json.has("owner"));
        assertFalse(json.toString().contains("private-password-hash"));
    }
}
