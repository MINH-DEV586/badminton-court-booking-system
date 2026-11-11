package com.badmintonhub.backend.repository;

import com.badmintonhub.backend.entity.Venue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VenueRepository extends JpaRepository<Venue, UUID> {
    Page<Venue> findByOwner_Id(UUID ownerId, Pageable pageable);
}
