package com.cgv.bookingservice.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/** Active allocation only. Cancelled/refunded bookings keep history in booking_seats. */
@Entity
@Table(name = "booking_seat_claims", uniqueConstraints = @UniqueConstraint(
        name = "uk_booking_seat_claim_showtime_seat", columnNames = {"showtime_id", "seat_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SeatClaim {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "showtime_id", nullable = false) private UUID showtimeId;
    @Column(name = "seat_id", nullable = false) private UUID seatId;
    @Column(name = "booking_id", nullable = false) private UUID bookingId;
}
