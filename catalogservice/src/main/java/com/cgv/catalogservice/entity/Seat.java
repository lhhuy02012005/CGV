package com.cgv.catalogservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Table(name = "seats",
        indexes = {
                @Index(name = "idx_seats_room_id_row_char_seat_number", columnList = "room_id, row_char, seat_number", unique = true),
                @Index(name = "idx_seats_room_id", columnList = "room_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    Room room;

    @Column(name = "row_char", length = 2, nullable = false)
    String rowChar;

    @Column(name = "seat_number", nullable = false)
    Integer seatNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_type_name", nullable = false)
    SeatType seatType;

    @Column(name = "is_active")
    @Builder.Default
    Boolean isActive = true;
}
