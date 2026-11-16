package com.badmintonhub.backend.controller;

import com.badmintonhub.backend.dto.response.VenueResponse;
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

@ContextConfiguration(classes = VenueController.class)
class VenueControllerTests extends ControllerTestSupport {
    private static final String BODY = "{\"name\":\"Venue A\",\"address\":\"Ha Noi\",\"timeZone\":\"Asia/Ho_Chi_Minh\"}";

    @Test
    void ownerCreatesVenueWithLocationAndCustomerCannotCreate() throws Exception {
        var id = UUID.randomUUID();
        when(venues.createVenue(any())).thenReturn(new VenueResponse(id, callerId, "Venue A", "Ha Noi", "UTC", Instant.now()));
        mvc.perform(post("/api/owner/venues").with(identity("OWNER")).contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/venues/" + id));
        mvc.perform(post("/api/owner/venues").with(identity("CUSTOMER")).contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isForbidden());
        verify(venues, times(1)).createVenue(any());
    }

    @Test
    void validatesBodyBeforeCallingService() throws Exception {
        mvc.perform(post("/api/owner/venues").with(identity("OWNER")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\" \",\"address\":\"Ha Noi\",\"timeZone\":\"Unknown/Zone\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.timeZone").exists());
        verifyNoInteractions(venues);
    }

    @Test
    void paginatesAndUsesCurrentOwnerIdentity() throws Exception {
        when(venues.getOwnerVenues(eq(callerId), any())).thenReturn(Page.empty());
        mvc.perform(get("/api/owner/venues?page=1&size=10").with(identity("OWNER")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
        verify(venues).getOwnerVenues(eq(callerId), argThat(page -> page.getPageNumber() == 1 && page.getPageSize() == 10));
    }

    @Test
    void rejectsInvalidPaginationAndMalformedIds() throws Exception {
        mvc.perform(get("/api/venues?size=101").with(identity("CUSTOMER"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/venues/not-a-uuid").with(identity("CUSTOMER"))).andExpect(status().isBadRequest());
        verifyNoInteractions(venues);
    }

    @Test
    void deleteIsNoContentAndAnonymousReadsAreUnauthorized() throws Exception {
        var id = UUID.randomUUID();
        mvc.perform(delete("/api/owner/venues/{id}", id).with(identity("ADMIN"))).andExpect(status().isNoContent());
        verify(venues).deleteVenue(id);
        mvc.perform(get("/api/venues")).andExpect(status().isUnauthorized());
    }
}
