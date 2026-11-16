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

@ContextConfiguration(classes = BookingController.class)
class BookingControllerTests extends ControllerTestSupport {

    @Test
    void createUsesServerOwnedCustomerAndReturnsLocation() throws Exception {
        var id = UUID.randomUUID(); var courtId = UUID.randomUUID();
        var start = Instant.parse("2099-01-01T08:00:00Z");
        when(bookings.createBooking(any())).thenReturn(new BookingResponse(id, callerId, courtId, start,
            start.plusSeconds(3600), Booking.Status.PENDING_PAYMENT, 120000, Instant.now().plusSeconds(900), Instant.now()));
        mvc.perform(post("/api/bookings").with(identity("CUSTOMER")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"courtId\":\""+courtId+"\",\"startsAt\":\"2099-01-01T08:00:00Z\",\"endsAt\":\"2099-01-01T09:00:00Z\",\"customerId\":\""+UUID.randomUUID()+"\",\"totalAmountVnd\":1}"))
            .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/bookings/"+id))
            .andExpect(jsonPath("$.totalAmountVnd").value(120000));
    }

    @Test
    void invalidTimeRangeNeverCallsBookingService() throws Exception {
        mvc.perform(post("/api/bookings").with(identity("CUSTOMER")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"courtId\":\""+UUID.randomUUID()+"\",\"startsAt\":\"2099-01-01T10:00:00Z\",\"endsAt\":\"2099-01-01T09:00:00Z\"}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(bookings);
    }

    @Test
    void myBookingsUseJwtAndBatchExpiryIsAdminOnly() throws Exception {
        when(bookings.getCustomerBookings(eq(callerId), any())).thenReturn(Page.empty());
        mvc.perform(get("/api/bookings/me").with(identity("CUSTOMER"))).andExpect(status().isOk());
        verify(bookings).getCustomerBookings(eq(callerId), any());
        mvc.perform(post("/api/admin/bookings/expire-pending").with(identity("OWNER"))).andExpect(status().isForbidden());
        when(bookings.expirePendingBookings()).thenReturn(3);
        mvc.perform(post("/api/admin/bookings/expire-pending").with(identity("ADMIN")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.expiredCount").value(3));
        verify(bookings, times(1)).expirePendingBookings();
    }

    @Test
    void propagatesAccessDeniedWithoutReturningBookingData() throws Exception {
        var id = UUID.randomUUID();
        when(bookings.getBooking(id)).thenThrow(new org.springframework.security.access.AccessDeniedException("Other customer"));
        mvc.perform(get("/api/bookings/{id}", id).with(identity("CUSTOMER")))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

}
