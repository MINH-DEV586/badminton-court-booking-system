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

@ContextConfiguration(classes = PaymentController.class)
class PaymentControllerTests extends ControllerTestSupport {

    @Test
    void createReturnsServerAmountAndLocation() throws Exception {
        var id = UUID.randomUUID(); var bookingId = UUID.randomUUID();
        when(payments.createPayment(any())).thenReturn(new PaymentResponse(id, bookingId, "VNPAY", null,
            120000, Payment.Status.PENDING, null, Instant.now()));
        mvc.perform(post("/api/payments").with(identity("CUSTOMER")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"bookingId\":\""+bookingId+"\",\"provider\":\"VNPAY\",\"amountVnd\":1,\"status\":\"SUCCEEDED\"}"))
            .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/payments/"+id))
            .andExpect(jsonPath("$.amountVnd").value(120000)).andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void customerAndOwnerCannotMarkPaymentsCompletedOrFailed() throws Exception {
        var id = UUID.randomUUID();
        for (var role : List.of("CUSTOMER", "OWNER")) {
            mvc.perform(post("/api/admin/payments/{id}/complete", id).with(identity(role))
                .contentType(MediaType.APPLICATION_JSON).content("{\"providerPaymentId\":\"ref\"}")).andExpect(status().isForbidden());
            mvc.perform(post("/api/admin/payments/{id}/fail", id).with(identity(role))).andExpect(status().isForbidden());
        }
        verifyNoInteractions(payments);
    }

    @Test
    void administratorMustSupplyBoundedNonblankProviderReference() throws Exception {
        var id = UUID.randomUUID();
        for (var reference : List.of(" ", "a".repeat(129))) {
            mvc.perform(post("/api/admin/payments/{id}/complete", id).with(identity("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"providerPaymentId\":\""+reference+"\"}"))
                .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/admin/payments/{id}/complete", id).with(identity("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"providerPaymentId\":\"ref-1\"}")).andExpect(status().isOk());
        verify(payments).completePayment(id, "ref-1");
    }

    @Test
    void bookingPaymentListIsPagedAndCustomerCannotRefund() throws Exception {
        var bookingId = UUID.randomUUID();
        when(payments.getPaymentsForBooking(eq(bookingId), any())).thenReturn(Page.empty());
        mvc.perform(get("/api/bookings/{id}/payments?size=10", bookingId).with(identity("CUSTOMER")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
        mvc.perform(post("/api/owner/payments/{id}/refund", UUID.randomUUID()).with(identity("CUSTOMER")))
            .andExpect(status().isForbidden());
        verify(payments, never()).refundPayment(any());
    }

}
