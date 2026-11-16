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

import com.badmintonhub.backend.service.BookingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api")
public class BookingController {
    private final BookingService bookings;
    public BookingController(BookingService bookings) { this.bookings = bookings; }

    @PostMapping("/bookings")
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        var response = bookings.createBooking(request);
        return ResponseEntity.created(URI.create("/api/bookings/" + response.id())).body(response);
    }

    @GetMapping("/bookings/me")
    public PagedModel<BookingResponse> getMyBookings(@AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return new PagedModel<>(bookings.getCustomerBookings(UUID.fromString(jwt.getSubject()),
            PageRequest.of(page, size, Sort.by("startsAt").descending())));
    }

    @GetMapping("/bookings/{id}")
    public BookingResponse getBooking(@PathVariable UUID id) { return bookings.getBooking(id); }

    @PostMapping("/bookings/{id}/cancel")
    public BookingResponse cancelBooking(@PathVariable UUID id) { return bookings.cancelBooking(id); }

    @GetMapping("/owner/courts/{courtId}/bookings")
    public PagedModel<BookingResponse> getCourtBookings(@PathVariable UUID courtId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return new PagedModel<>(bookings.getCourtBookings(courtId,
            PageRequest.of(page, size, Sort.by("startsAt").descending())));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users/{customerId}/bookings")
    public PagedModel<BookingResponse> getCustomerBookings(@PathVariable UUID customerId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return new PagedModel<>(bookings.getCustomerBookings(customerId,
            PageRequest.of(page, size, Sort.by("startsAt").descending())));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/bookings/expire-pending")
    public ExpirationResponse expirePendingBookings() {
        return new ExpirationResponse(bookings.expirePendingBookings());
    }

    public record ExpirationResponse(int expiredCount) {}
}
