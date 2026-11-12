package com.badmintonhub.backend.dto;

import com.badmintonhub.backend.dto.response.CourtBlockResponse;
import com.badmintonhub.backend.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class CourtBlockResponseTests {
    @Test
    void mapsAllFieldsAndSerializesFlatResponse() {
        var id = UUID.randomUUID();
        var courtId = UUID.randomUUID();
        var court = new Court();
        ReflectionTestUtils.setField(court, "id", courtId);
        var createdById = UUID.randomUUID();
        var createdBy = new UserAccount();
        ReflectionTestUtils.setField(createdBy, "id", createdById);
        createdBy.setPasswordHash("private-password-hash");
        var entity = new CourtBlock();
        ReflectionTestUtils.setField(entity, "id", id);
        ReflectionTestUtils.setField(entity, "startsAt", Instant.parse("2026-11-12T08:00:00Z"));
        ReflectionTestUtils.setField(entity, "endsAt", Instant.parse("2026-11-12T10:00:00Z"));
        ReflectionTestUtils.setField(entity, "reason", "Maintenance");
        ReflectionTestUtils.setField(entity, "createdAt", Instant.parse("2026-11-11T08:00:00Z"));
        ReflectionTestUtils.setField(entity, "cancelledAt", null);
        ReflectionTestUtils.setField(entity, "court", court);
        ReflectionTestUtils.setField(entity, "createdBy", createdBy);
        var response = CourtBlockResponse.from(entity);
        assertEquals(new CourtBlockResponse(id, courtId, createdById, Instant.parse("2026-11-12T08:00:00Z"), Instant.parse("2026-11-12T10:00:00Z"), "Maintenance", Instant.parse("2026-11-11T08:00:00Z"), null), response);

        var json = JsonMapper.builder().build().valueToTree(response);
        assertEquals(8, json.size());
        assertTrue(json.has("id"));
        assertTrue(json.has("courtId"));
        assertTrue(json.has("createdById"));
        assertTrue(json.has("startsAt"));
        assertTrue(json.has("endsAt"));
        assertTrue(json.has("reason"));
        assertTrue(json.has("createdAt"));
        assertTrue(json.has("cancelledAt"));
        assertEquals(courtId.toString(), json.get("courtId").asText());
        assertFalse(json.has("court"));
        assertEquals(createdById.toString(), json.get("createdById").asText());
        assertFalse(json.has("createdBy"));
        assertFalse(json.toString().contains("private-password-hash"));
    }
}
