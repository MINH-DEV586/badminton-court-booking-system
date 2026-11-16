package com.badmintonhub.backend.controller;

import com.badmintonhub.backend.dto.request.*;
import com.badmintonhub.backend.dto.response.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.*;

import com.badmintonhub.backend.service.PaymentService;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api")
public class PaymentController {
    private final PaymentService payments;
    public PaymentController(PaymentService payments) { this.payments = payments; }

    @PostMapping("/payments")
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        var response = payments.createPayment(request);
        return ResponseEntity.created(URI.create("/api/payments/" + response.id())).body(response);
    }

    @GetMapping("/payments/{id}")
    public PaymentResponse getPayment(@PathVariable UUID id) { return payments.getPayment(id); }

    @GetMapping("/bookings/{bookingId}/payments")
    public PagedModel<PaymentResponse> getPayments(@PathVariable UUID bookingId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return new PagedModel<>(payments.getPaymentsForBooking(bookingId,
            PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    // Administrative reconciliation only; these are not public payment-provider webhooks.
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/payments/{id}/complete")
    public PaymentResponse completePayment(@PathVariable UUID id,
        @Valid @RequestBody CompletePaymentRequest request) {
        return payments.completePayment(id, request.providerPaymentId());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/payments/{id}/fail")
    public PaymentResponse failPayment(@PathVariable UUID id) { return payments.failPayment(id); }

    @PostMapping("/owner/payments/{id}/refund")
    public PaymentResponse refundPayment(@PathVariable UUID id) { return payments.refundPayment(id); }

    public record CompletePaymentRequest(@NotBlank @Size(max = 128) String providerPaymentId) {}
}
