package com.cgv.bookingservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "booking_seats" , indexes = {
        @Index(name = "idx_cgv_booking_seat_booking_id_seat_id" , columnList = "seat_id, booking_id")
},uniqueConstraints = {
        @UniqueConstraint(name = "uk_cgv_booking_seat_showtime_id_seat_id", columnNames = {"showtime_id","seat_id"})
})
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingSeat extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    Booking booking;

    @Column(nullable = false)
    UUID seatId;

    @Column(nullable = false)
    UUID showTimeId;

    @Column(nullable = false)
    BigDecimal price;

    @Column
    String seatLabel;



}
