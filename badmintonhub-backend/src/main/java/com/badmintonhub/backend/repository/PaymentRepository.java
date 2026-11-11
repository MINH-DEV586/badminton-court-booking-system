package com.badmintonhub.backend.repository;

import com.badmintonhub.backend.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Page<Payment> findByBooking_Id(UUID bookingId, Pageable pageable);

    Optional<Payment> findByProviderAndProviderPaymentId(String provider, String providerPaymentId);
}
