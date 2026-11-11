package com.badmintonhub.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "venues")
@Getter
@Setter
@NoArgsConstructor
public class Venue {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", nullable = false, updatable = false, length = 16)
    @Setter(AccessLevel.NONE)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
