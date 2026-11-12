package com.badmintonhub.backend.dto.response;

import com.badmintonhub.backend.entity.Payment;
import java.time.*;
import java.util.UUID;

/** Map within the service transaction while required entity relationships are available. */
public record PaymentResponse(
    UUID id,
    UUID bookingId,
    String provider,
    String providerPaymentId,
    long amountVnd,
    Payment.Status status,
    Instant paidAt,
    Instant createdAt
) {
    public static PaymentResponse from(Payment entity) {
        return new PaymentResponse(
            entity.getId(),
            entity.getBooking().getId(),
            entity.getProvider(),
            entity.getProviderPaymentId(),
            entity.getAmountVnd(),
            entity.getStatus(),
            entity.getPaidAt(),
            entity.getCreatedAt());
    }
}
