package com.badmintonhub.backend.service;

import com.badmintonhub.backend.dto.request.CreateCourtBlockRequest;
import com.badmintonhub.backend.dto.response.CourtBlockResponse;
import com.badmintonhub.backend.entity.Court;
import com.badmintonhub.backend.entity.CourtBlock;
import com.badmintonhub.backend.exception.ApiException;
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

import java.time.Instant;
import java.util.UUID;

@Service
@Validated
public class CourtBlockService {
    private final CourtBlockRepository courtBlocks;
    private final CourtRepository courts;
    private final UserAccountRepository users;

    public CourtBlockService(CourtBlockRepository courtBlocks, CourtRepository courts, UserAccountRepository users) {
        this.courtBlocks = courtBlocks;
        this.courts = courts;
        this.users = users;
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Transactional
    public CourtBlockResponse createBlock(@NotNull @Valid CreateCourtBlockRequest request) {
        var court = courts.findById(request.courtId()).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "COURT_NOT_FOUND", "Court not found"));
        checkCourtOwnership(court);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString(auth.getName());
        var creator = users.findById(userId)
            .filter(user -> user.isActive())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authenticated user not found"));

        if (courtBlocks.existsConflictingBlock(court.getId(), request.startsAt(), request.endsAt())) {
            throw new ApiException(HttpStatus.CONFLICT, "BLOCK_CONFLICT", "A court block already exists for the selected time range");
        }

        var block = new CourtBlock();
        block.setCourt(court);
        block.setCreatedBy(creator);
        block.setStartsAt(request.startsAt());
        block.setEndsAt(request.endsAt());
        block.setReason(request.reason().strip());

        return CourtBlockResponse.from(courtBlocks.save(block));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Transactional
    public CourtBlockResponse cancelBlock(@NotNull UUID id) {
        var block = courtBlocks.findById(id).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "COURT_BLOCK_NOT_FOUND", "Court block not found"));
        checkCourtOwnership(block.getCourt());

        if (block.getCancelledAt() == null) {
            block.setCancelledAt(Instant.now());
        }

        return CourtBlockResponse.from(block);
    }

    @Transactional(readOnly = true)
    public CourtBlockResponse getBlock(@NotNull UUID id) {
        var block = courtBlocks.findById(id).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "COURT_BLOCK_NOT_FOUND", "Court block not found"));
        return CourtBlockResponse.from(block);
    }

    @Transactional(readOnly = true)
    public Page<CourtBlockResponse> getActiveBlocksForCourt(@NotNull UUID courtId, Pageable pageable) {
        return courtBlocks.findByCourt_IdAndCancelledAtIsNull(courtId, pageable)
            .map(CourtBlockResponse::from);
    }

    private void checkCourtOwnership(Court court) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var admin = auth.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && !court.getVenue().getOwner().getId().toString().equals(auth.getName())) {
            throw new AccessDeniedException("Only the venue owner or an administrator can manage blocks for this court");
        }
    }
}
