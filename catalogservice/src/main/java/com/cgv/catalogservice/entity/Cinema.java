package com.cgv.catalogservice.entity;

import com.cgv.catalogservice.enums.CinemaStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Table(name = "cinemas",
       indexes = {
            @Index(name = "idx_cinema_region_id", columnList = "region_id"),
            @Index(name = "idx_cinema_status", columnList = "status")
       }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Cinema extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    Region region;

    @Column(unique = true, nullable = false)
    String name;

    String address;

    @Column(length = 20)
    String phone;

    @Column(name = "opening_hours")
    String openingHours;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    CinemaStatus status = CinemaStatus.ACTIVE;
}
