package com.badmintonhub.backend.service;

import com.badmintonhub.backend.dto.request.CreatePaymentRequest;
import com.badmintonhub.backend.dto.response.PaymentResponse;
import com.badmintonhub.backend.entity.Booking;
import com.badmintonhub.backend.entity.Payment;
import com.badmintonhub.backend.exception.ApiException;
import com.badmintonhub.backend.repository.BookingRepository;
import com.badmintonhub.backend.repository.PaymentRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.UUID;

@Service
@Validated
public class PaymentService {
    private final PaymentRepository payments;
    private final BookingRepository bookings;

    public PaymentService(PaymentRepository payments, BookingRepository bookings) {
        this.payments = payments;
        this.bookings = bookings;
    }

    @Transactional
    public PaymentResponse createPayment(@NotNull @Valid CreatePaymentRequest request) {
        var booking = bookings.findById(request.bookingId()).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", "Booking not found"));
        checkBookingAccess(booking);

        if (booking.getStatus() != Booking.Status.PENDING_PAYMENT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_BOOKING_STATUS",
                "Booking is not awaiting payment");
        }

        if (booking.getHoldExpiresAt() != null && booking.getHoldExpiresAt().isBefore(Instant.now())) {
            booking.setStatus(Booking.Status.EXPIRED);
            throw new ApiException(HttpStatus.GONE, "BOOKING_HOLD_EXPIRED",
                "Booking hold has expired. Please create a new booking");
        }

        var payment = new Payment();
        payment.setBooking(booking);
        payment.setProvider(request.provider().strip());
        payment.setAmountVnd(booking.getTotalAmountVnd());
        payment.setStatus(Payment.Status.PENDING);

        return PaymentResponse.from(payments.save(payment));
    }

    @Transactional
    public PaymentResponse completePayment(@NotNull UUID id, @NotBlank String providerPaymentId) {
        var payment = payments.findById(id).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found"));

        if (payment.getStatus() == Payment.Status.SUCCEEDED) {
            return PaymentResponse.from(payment);
        }

        payment.setStatus(Payment.Status.SUCCEEDED);
        payment.setProviderPaymentId(providerPaymentId.strip());
        payment.setPaidAt(Instant.now());

        var booking = payment.getBooking();
        booking.setStatus(Booking.Status.CONFIRMED);
        booking.setHoldExpiresAt(null);

        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse failPayment(@NotNull UUID id) {
        var payment = payments.findById(id).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found"));

        payment.setStatus(Payment.Status.FAILED);
        return PaymentResponse.from(payment);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Transactional
    public PaymentResponse refundPayment(@NotNull UUID id) {
        var payment = payments.findById(id).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found"));

        if (payment.getStatus() != Payment.Status.SUCCEEDED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CANNOT_REFUND",
                "Only succeeded payments can be refunded");
        }

        var booking = payment.getBooking();
        checkCourtManageAccess(booking.getCourt().getVenue().getOwner().getId().toString());

        payment.setStatus(Payment.Status.REFUNDED);
        booking.setStatus(Booking.Status.CANCELLED);

        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(@NotNull UUID id) {
        var payment = payments.findById(id).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found"));
        checkBookingAccess(payment.getBooking());
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsForBooking(@NotNull UUID bookingId, Pageable pageable) {
        var booking = bookings.findById(bookingId).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", "Booking not found"));
        checkBookingAccess(booking);
        return payments.findByBooking_Id(bookingId, pageable).map(PaymentResponse::from);
    }

    private void checkBookingAccess(Booking booking) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var admin = auth.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        var callerId = auth.getName();
        boolean isCustomer = booking.getCustomer().getId().toString().equals(callerId);
        boolean isOwner = booking.getCourt().getVenue().getOwner().getId().toString().equals(callerId);

        if (!admin && !isCustomer && !isOwner) {
            throw new AccessDeniedException("Not authorized to access payments for this booking");
        }
    }

    private void checkCourtManageAccess(String venueOwnerId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var admin = auth.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && !venueOwnerId.equals(auth.getName())) {
            throw new AccessDeniedException("Only the venue owner or an administrator can refund payments");
        }
    }
}
