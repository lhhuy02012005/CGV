package com.cgv.catalogservice.entity;

import com.cgv.catalogservice.converter.FormatConverter;
import com.cgv.catalogservice.enums.Format;
import com.cgv.catalogservice.enums.RoomStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cinema_id", nullable = false)
    Cinema cinema;

    @Column(nullable = false)
    String name;

    @Convert(converter = FormatConverter.class)
    @Builder.Default
    Format format = Format.TWO_D;

    @Column(name = "total_seats")
    @Builder.Default
    Integer totalSeats = 0;

    @Column(name = "row_count", nullable = false)
    Integer rowCount;

    @Column(name = "column_count", nullable = false)
    Integer columnCount;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    RoomStatus status = RoomStatus.ACTIVE;
}
