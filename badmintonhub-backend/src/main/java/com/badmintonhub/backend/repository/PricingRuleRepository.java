package com.badmintonhub.backend.repository;

import com.badmintonhub.backend.entity.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PricingRuleRepository extends JpaRepository<PricingRule, UUID> {
    List<PricingRule> findByCourt_IdAndDayOfWeekOrderByStartLocalTimeAsc(UUID courtId, int dayOfWeek);
}
