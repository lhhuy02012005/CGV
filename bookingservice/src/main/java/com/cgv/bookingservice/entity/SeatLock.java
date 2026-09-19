package com.cgv.bookingservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name ="seat_locks" , uniqueConstraints = {
        @UniqueConstraint(name = "uk_cgv_showtime_id_seat_id", columnNames = {"showtime_id", "seat_id"})
},indexes = {
        @Index(name = "idx_cgv_seat_lock_user_id" , columnList = "user_id"),
        @Index(name = "idx_cgv_seat_lock_expires_at", columnList = "expires_at"),
        @Index(name = "idx_cgv_seat_lock_status" , columnList = "status")
})
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeatLock extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false)
    UUID showTimeId;

    @Column(nullable = false)
    UUID seatId;

    @Column
    String sessionId;

    @Column
    Instant expiresAt;

}
