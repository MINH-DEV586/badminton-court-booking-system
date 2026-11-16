package com.badmintonhub.backend.repository;

import com.badmintonhub.backend.entity.CourtBlock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface CourtBlockRepository extends JpaRepository<CourtBlock, UUID> {
    Page<CourtBlock> findByCourt_IdAndCancelledAtIsNull(UUID courtId, Pageable pageable);

    @Query("SELECT COUNT(cb) > 0 FROM CourtBlock cb " +
           "WHERE cb.court.id = :courtId " +
           "AND cb.cancelledAt IS NULL " +
           "AND cb.startsAt < :endsAt " +
           "AND cb.endsAt > :startsAt")
    boolean existsConflictingBlock(
        @Param("courtId") UUID courtId,
        @Param("startsAt") Instant startsAt,
        @Param("endsAt") Instant endsAt);
}
