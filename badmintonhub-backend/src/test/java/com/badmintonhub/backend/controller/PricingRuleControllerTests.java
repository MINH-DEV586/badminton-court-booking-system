package com.badmintonhub.backend.controller;

import com.badmintonhub.backend.auth.AuthDtos;
import com.badmintonhub.backend.dto.request.*;
import com.badmintonhub.backend.dto.response.*;
import com.badmintonhub.backend.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import java.util.*;
import java.time.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ContextConfiguration(classes = PricingRuleController.class)
class PricingRuleControllerTests extends ControllerTestSupport {

    @Test
    void filtersByIsoDayAndRejectsInvalidDay() throws Exception {
        var courtId = UUID.randomUUID();
        when(pricing.getRulesForCourtAndDay(courtId, 7)).thenReturn(List.of());
        mvc.perform(get("/api/courts/{id}/pricing-rules?dayOfWeek=7", courtId).with(identity("CUSTOMER")))
            .andExpect(status().isOk());
        verify(pricing).getRulesForCourtAndDay(courtId, 7);
        mvc.perform(get("/api/courts/{id}/pricing-rules?dayOfWeek=8", courtId).with(identity("CUSTOMER")))
            .andExpect(status().isBadRequest());
        verify(pricing, times(1)).getRulesForCourtAndDay(any(), anyInt());
    }

    @Test
    void validatesCrossFieldTimesBeforeCreatingRule() throws Exception {
        mvc.perform(post("/api/owner/pricing-rules").with(identity("OWNER")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"courtId\":\""+UUID.randomUUID()+"\",\"dayOfWeek\":1,\"startLocalTime\":\"10:00:00\",\"endLocalTime\":\"09:00:00\",\"hourlyRateVnd\":0,\"validFrom\":\"2026-01-01\"}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(pricing);
    }

    @Test
    void createReturnsLocationAndCustomerCannotDelete() throws Exception {
        var id = UUID.randomUUID(); var courtId = UUID.randomUUID();
        when(pricing.createRule(any())).thenReturn(new PricingRuleResponse(id, courtId, 1,
            LocalTime.of(9,0), LocalTime.of(10,0), 150000, LocalDate.of(2026,1,1), null));
        mvc.perform(post("/api/owner/pricing-rules").with(identity("OWNER")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"courtId\":\""+courtId+"\",\"dayOfWeek\":1,\"startLocalTime\":\"09:00:00\",\"endLocalTime\":\"10:00:00\",\"hourlyRateVnd\":150000,\"validFrom\":\"2026-01-01\"}"))
            .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/pricing-rules/"+id));
        mvc.perform(delete("/api/owner/pricing-rules/{id}", id).with(identity("CUSTOMER"))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/owner/pricing-rules/{id}", id).with(identity("ADMIN"))).andExpect(status().isNoContent());
        verify(pricing).deleteRule(id);
    }

}
