package com.badmintonhub.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "pricing_rules", indexes = @Index(name = "idx_pricing_rules_court_day", columnList = "court_id, day_of_week, valid_from"))
@Check(constraints = "day_of_week between 1 and 7 and start_local_time < end_local_time and hourly_rate_vnd >= 0 and (valid_until is null or valid_from < valid_until)")
@Getter
@Setter
@NoArgsConstructor
public class PricingRule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", nullable = false, updatable = false, length = 16)
    @Setter(AccessLevel.NONE)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    @Column(name = "start_local_time", nullable = false)
    private LocalTime startLocalTime;

    @Column(name = "end_local_time", nullable = false)
    private LocalTime endLocalTime;

    @Column(name = "hourly_rate_vnd", nullable = false)
    private long hourlyRateVnd;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;
}
