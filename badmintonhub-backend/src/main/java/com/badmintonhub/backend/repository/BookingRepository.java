package com.badmintonhub.backend.repository;

import com.badmintonhub.backend.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    Page<Booking> findByCustomer_Id(UUID customerId, Pageable pageable);

    Page<Booking> findByCourt_Id(UUID courtId, Pageable pageable);

    Page<Booking> findByStatus(Booking.Status status, Pageable pageable);
}
