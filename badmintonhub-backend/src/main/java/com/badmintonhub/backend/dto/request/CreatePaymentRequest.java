package com.badmintonhub.backend.dto.request;

import jakarta.validation.constraints.*;
import java.util.UUID;

/** Amount comes from the booking; provider reference and status come from the payment provider. */
public record CreatePaymentRequest(
    @NotNull UUID bookingId,
    @NotBlank @Size(max = 32) String provider
) {
}
