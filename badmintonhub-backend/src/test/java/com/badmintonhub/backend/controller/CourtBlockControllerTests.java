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

@ContextConfiguration(classes = CourtBlockController.class)
class CourtBlockControllerTests extends ControllerTestSupport {

    @Test
    void ownerCreatesBlockAndGetsResourceLocation() throws Exception {
        var id = UUID.randomUUID(); var courtId = UUID.randomUUID();
        when(blocks.createBlock(any())).thenReturn(new CourtBlockResponse(id, courtId, callerId,
            Instant.parse("2099-01-01T08:00:00Z"), Instant.parse("2099-01-01T09:00:00Z"), "Maintenance", Instant.now(), null));
        mvc.perform(post("/api/owner/court-blocks").with(identity("OWNER")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"courtId\":\""+courtId+"\",\"startsAt\":\"2099-01-01T08:00:00Z\",\"endsAt\":\"2099-01-01T09:00:00Z\",\"reason\":\"Maintenance\"}"))
            .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/court-blocks/"+id));
    }

    @Test
    void rejectsBlankReasonAndCustomerCancellation() throws Exception {
        mvc.perform(post("/api/owner/court-blocks").with(identity("OWNER")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"courtId\":\""+UUID.randomUUID()+"\",\"startsAt\":\"2099-01-01T08:00:00Z\",\"endsAt\":\"2099-01-01T09:00:00Z\",\"reason\":\" \"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/owner/court-blocks/{id}/cancel", UUID.randomUUID()).with(identity("CUSTOMER")))
            .andExpect(status().isForbidden());
        verifyNoInteractions(blocks);
    }

    @Test
    void listsActiveBlocksWithBoundedPaginationAndOwnerCanCancel() throws Exception {
        var courtId = UUID.randomUUID();
        when(blocks.getActiveBlocksForCourt(eq(courtId), any())).thenReturn(Page.empty());
        mvc.perform(get("/api/courts/{id}/blocks?size=10", courtId).with(identity("CUSTOMER")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
        var id = UUID.randomUUID();
        mvc.perform(post("/api/owner/court-blocks/{id}/cancel", id).with(identity("OWNER"))).andExpect(status().isOk());
        verify(blocks).cancelBlock(id);
        mvc.perform(get("/api/courts/{id}/blocks?page=-1", courtId).with(identity("CUSTOMER"))).andExpect(status().isBadRequest());
    }

}
