package com.badmintonhub.backend.repository;

import com.badmintonhub.backend.entity.Court;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CourtRepository extends JpaRepository<Court, UUID> {
    Page<Court> findByVenue_Id(UUID venueId, Pageable pageable);

    Page<Court> findByVenue_IdAndActiveTrue(UUID venueId, Pageable pageable);
}
