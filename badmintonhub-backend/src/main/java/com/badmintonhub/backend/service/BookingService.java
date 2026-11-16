package com.badmintonhub.backend.service;

import com.badmintonhub.backend.dto.request.CreateBookingRequest;
import com.badmintonhub.backend.dto.response.BookingResponse;
import com.badmintonhub.backend.entity.Booking;
import com.badmintonhub.backend.entity.Court;
import com.badmintonhub.backend.exception.ApiException;
import com.badmintonhub.backend.repository.BookingRepository;
import com.badmintonhub.backend.repository.CourtBlockRepository;
import com.badmintonhub.backend.repository.CourtRepository;
import com.badmintonhub.backend.repository.UserAccountRepository;
import jakarta.validation.Valid;
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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Validated
public class BookingService {
    public static final Duration HOLD_DURATION = Duration.ofMinutes(15);

    private final BookingRepository bookings;
    private final CourtRepository courts;
    private final UserAccountRepository users;
    private final CourtBlockRepository courtBlocks;
    private final PricingRuleService pricingRuleService;

    public BookingService(BookingRepository bookings,
                          CourtRepository courts,
                          UserAccountRepository users,
                          CourtBlockRepository courtBlocks,
                          PricingRuleService pricingRuleService) {
        this.bookings = bookings;
        this.courts = courts;
        this.users = users;
        this.courtBlocks = courtBlocks;
        this.pricingRuleService = pricingRuleService;
    }

    @Transactional
    public BookingResponse createBooking(@NotNull @Valid CreateBookingRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UUID customerId = UUID.fromString(auth.getName());
        var customer = users.findById(customerId)
            .filter(user -> user.isActive())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authenticated customer not found"));

        var court = courts.findById(request.courtId()).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "COURT_NOT_FOUND", "Court not found"));

        if (!court.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "COURT_INACTIVE", "Court is currently inactive");
        }

        Instant now = Instant.now();

        // Check if court is blocked by maintenance/schedule
        if (courtBlocks.existsConflictingBlock(court.getId(), request.startsAt(), request.endsAt())) {
            throw new ApiException(HttpStatus.CONFLICT, "COURT_BLOCKED", "Court is blocked for maintenance or reserved during the requested time");
        }

        // Check if another active booking overlaps with the requested range
        var activeStatuses = List.of(Booking.Status.CONFIRMED, Booking.Status.PENDING_PAYMENT);
        if (bookings.existsConflictingBooking(court.getId(), activeStatuses, request.startsAt(), request.endsAt(), now)) {
            throw new ApiException(HttpStatus.CONFLICT, "BOOKING_CONFLICT", "Court is already booked for the selected time range");
        }

        // Calculate price dynamically from pricing rules and court base rate
        long totalAmount = pricingRuleService.calculateTotalPrice(court, request.startsAt(), request.endsAt());

        var booking = new Booking();
        booking.setCustomer(customer);
        booking.setCourt(court);
        booking.setStartsAt(request.startsAt());
        booking.setEndsAt(request.endsAt());
        booking.setStatus(Booking.Status.PENDING_PAYMENT);
        booking.setTotalAmountVnd(totalAmount);
        booking.setHoldExpiresAt(now.plus(HOLD_DURATION));

        return BookingResponse.from(bookings.save(booking));
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(@NotNull UUID id) {
        var booking = bookings.findById(id).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", "Booking not found"));
        checkReadAccess(booking);
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getCustomerBookings(@NotNull UUID customerId, Pageable pageable) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var admin = auth.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && !customerId.toString().equals(auth.getName())) {
            throw new AccessDeniedException("Cannot view bookings belonging to another user");
        }
        return bookings.findByCustomer_Id(customerId, pageable).map(BookingResponse::from);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Transactional(readOnly = true)
    public Page<BookingResponse> getCourtBookings(@NotNull UUID courtId, Pageable pageable) {
        var court = courts.findById(courtId).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "COURT_NOT_FOUND", "Court not found"));
        checkCourtManageAccess(court);
        return bookings.findByCourt_Id(courtId, pageable).map(BookingResponse::from);
    }

    @Transactional
    public BookingResponse cancelBooking(@NotNull UUID id) {
        var booking = bookings.findById(id).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", "Booking not found"));
        checkCancelAccess(booking);

        if (booking.getStatus() == Booking.Status.CANCELLED) {
            return BookingResponse.from(booking);
        }
        if (booking.getStatus() == Booking.Status.COMPLETED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CANNOT_CANCEL", "Cannot cancel a completed booking");
        }
        if (booking.getStatus() == Booking.Status.CONFIRMED && booking.getStartsAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CANNOT_CANCEL", "Cannot cancel a booking that has already started");
        }

        booking.setStatus(Booking.Status.CANCELLED);
        booking.setHoldExpiresAt(null);

        return BookingResponse.from(booking);
    }

    @Transactional
    public int expirePendingBookings() {
        var expiredList = bookings.findByStatusAndHoldExpiresAtBefore(
            Booking.Status.PENDING_PAYMENT, Instant.now());
        for (var booking : expiredList) {
            booking.setStatus(Booking.Status.EXPIRED);
        }
        return expiredList.size();
    }

    private void checkReadAccess(Booking booking) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var admin = auth.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        var callerId = auth.getName();
        boolean isCustomer = booking.getCustomer().getId().toString().equals(callerId);
        boolean isOwner = booking.getCourt().getVenue().getOwner().getId().toString().equals(callerId);

        if (!admin && !isCustomer && !isOwner) {
            throw new AccessDeniedException("Not authorized to view this booking");
        }
    }

    private void checkCancelAccess(Booking booking) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var admin = auth.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        var callerId = auth.getName();
        boolean isCustomer = booking.getCustomer().getId().toString().equals(callerId);
        boolean isOwner = booking.getCourt().getVenue().getOwner().getId().toString().equals(callerId);

        if (!admin && !isCustomer && !isOwner) {
            throw new AccessDeniedException("Not authorized to cancel this booking");
        }
    }

    private void checkCourtManageAccess(Court court) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var admin = auth.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && !court.getVenue().getOwner().getId().toString().equals(auth.getName())) {
            throw new AccessDeniedException("Only the venue owner or an administrator can view all bookings for this court");
        }
    }
}
