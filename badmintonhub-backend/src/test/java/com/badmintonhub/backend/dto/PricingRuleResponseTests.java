package com.badmintonhub.backend.dto;

import com.badmintonhub.backend.dto.response.PricingRuleResponse;
import com.badmintonhub.backend.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PricingRuleResponseTests {
    @Test
    void mapsAllFieldsAndSerializesFlatResponse() {
        var id = UUID.randomUUID();
        var courtId = UUID.randomUUID();
        var court = new Court();
        ReflectionTestUtils.setField(court, "id", courtId);
        var entity = new PricingRule();
        ReflectionTestUtils.setField(entity, "id", id);
        ReflectionTestUtils.setField(entity, "dayOfWeek", 7);
        ReflectionTestUtils.setField(entity, "startLocalTime", LocalTime.of(8, 0));
        ReflectionTestUtils.setField(entity, "endLocalTime", LocalTime.of(10, 0));
        ReflectionTestUtils.setField(entity, "hourlyRateVnd", 150000L);
        ReflectionTestUtils.setField(entity, "validFrom", LocalDate.of(2026, 11, 12));
        ReflectionTestUtils.setField(entity, "validUntil", null);
        ReflectionTestUtils.setField(entity, "court", court);
        var response = PricingRuleResponse.from(entity);
        assertEquals(new PricingRuleResponse(id, courtId, 7, LocalTime.of(8, 0), LocalTime.of(10, 0), 150000L, LocalDate.of(2026, 11, 12), null), response);

        var json = JsonMapper.builder().build().valueToTree(response);
        assertEquals(8, json.size());
        assertTrue(json.has("id"));
        assertTrue(json.has("courtId"));
        assertTrue(json.has("dayOfWeek"));
        assertTrue(json.has("startLocalTime"));
        assertTrue(json.has("endLocalTime"));
        assertTrue(json.has("hourlyRateVnd"));
        assertTrue(json.has("validFrom"));
        assertTrue(json.has("validUntil"));
        assertEquals(courtId.toString(), json.get("courtId").asText());
        assertFalse(json.has("court"));
        assertFalse(json.toString().contains("private-password-hash"));
    }
}
