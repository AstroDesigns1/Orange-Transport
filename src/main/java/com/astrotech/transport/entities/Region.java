package com.astrotech.transport.entities;

import com.astrotech.transport.enums.RegionName;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.*;

@Entity
@Table(
        name = "regions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_regions_code",
                        columnNames = "code"
                ),
                @UniqueConstraint(
                        name = "uk_regions_name",
                        columnNames = "name"
                )
        },
        indexes = {
                @Index(
                        name = "idx_regions_active",
                        columnList = "active"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(
            nullable = false,
            length = 50
    )
    @Enumerated(EnumType.STRING)
    private RegionName name;


    @Column(
            nullable = false
    )
    @Builder.Default
    private boolean active = true;

    @Column(
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(
            mappedBy = "region",
            fetch = FetchType.LAZY
    )
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regional_supervisor_id", referencedColumnName = "id", nullable = false, unique = true)
    private User regionalSupervisor;

    @Builder.Default
    @OneToMany(mappedBy = "region", fetch = FetchType.LAZY)
    private List<Terminal> terminals = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
