package com.badmintonhub.backend.controller;

import com.badmintonhub.backend.dto.response.CourtResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ContextConfiguration(classes = CourtController.class)
class CourtControllerTests extends ControllerTestSupport {
    @Test
    void ownerCanCreateCourtAndCustomerCannot() throws Exception {
        var id = UUID.randomUUID();
        var venueId = UUID.randomUUID();
        var body = "{\"venueId\":\"" + venueId + "\",\"name\":\"Court A\",\"baseHourlyRateVnd\":120000}";
        when(courts.createCourt(any())).thenReturn(new CourtResponse(id, venueId, "Court A", 120000, true, Instant.now()));
        mvc.perform(post("/api/owner/courts").with(identity("OWNER")).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/courts/" + id));
        mvc.perform(post("/api/owner/courts").with(identity("CUSTOMER")).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
        verify(courts, times(1)).createCourt(any());
    }

    @Test
    void defaultsToActiveCourtsAndSupportsAllCourts() throws Exception {
        var venueId = UUID.randomUUID();
        when(courts.getActiveCourtsByVenue(eq(venueId), any())).thenReturn(Page.empty());
        when(courts.getCourtsByVenue(eq(venueId), any())).thenReturn(Page.empty());
        mvc.perform(get("/api/venues/{id}/courts", venueId).with(identity("CUSTOMER")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
        verify(courts).getActiveCourtsByVenue(eq(venueId), argThat(page -> page.getPageSize() == 20));
        mvc.perform(get("/api/venues/{id}/courts?activeOnly=false&page=1&size=10", venueId).with(identity("OWNER")))
            .andExpect(status().isOk());
        verify(courts).getCourtsByVenue(eq(venueId), argThat(page -> page.getPageNumber() == 1 && page.getPageSize() == 10));
    }

    @Test
    void rejectsMissingRateAndOutOfBoundsPageSize() throws Exception {
        mvc.perform(post("/api/owner/courts").with(identity("OWNER")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"venueId\":\"" + UUID.randomUUID() + "\",\"name\":\"Court A\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/venues/{id}/courts?size=101", UUID.randomUUID()).with(identity("CUSTOMER")))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(courts);
    }
}
