package com.badmintonhub.backend.repository;

import com.badmintonhub.backend.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    Page<Booking> findByCustomer_Id(UUID customerId, Pageable pageable);

    Page<Booking> findByCourt_Id(UUID courtId, Pageable pageable);

    Page<Booking> findByStatus(Booking.Status status, Pageable pageable);

    @Query("SELECT COUNT(b) > 0 FROM Booking b " +
           "WHERE b.court.id = :courtId " +
           "AND b.status IN :statuses " +
           "AND b.startsAt < :endsAt " +
           "AND b.endsAt > :startsAt " +
           "AND (b.status <> com.badmintonhub.backend.entity.Booking.Status.PENDING_PAYMENT OR b.holdExpiresAt > :now)")
    boolean existsConflictingBooking(
        @Param("courtId") UUID courtId,
        @Param("statuses") Collection<Booking.Status> statuses,
        @Param("startsAt") Instant startsAt,
        @Param("endsAt") Instant endsAt,
        @Param("now") Instant now);

    List<Booking> findByStatusAndHoldExpiresAtBefore(Booking.Status status, Instant now);
}
