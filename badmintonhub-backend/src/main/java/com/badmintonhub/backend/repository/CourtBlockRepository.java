package com.badmintonhub.backend.repository;

import com.badmintonhub.backend.entity.CourtBlock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CourtBlockRepository extends JpaRepository<CourtBlock, UUID> {
    Page<CourtBlock> findByCourt_IdAndCancelledAtIsNull(UUID courtId, Pageable pageable);
}
