package com.cgv.bookingservice.entity;

import com.cgv.bookingservice.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_cgv_booking_user_id", columnList = "user_id"),
        @Index(name = "idx_cgv_booking_showtime_id", columnList = "showtime_id"),
        @Index(name = "idx_cgv_booking_status", columnList = "status"),
        @Index(name = "idx_cgv_booking_payment_deadline", columnList = "payment_deadline"),
        @Index(name = "idx_cgv_booking_created_at", columnList = "created_at"),
        @Index(name = "idx_cgv_booking_guest_email", columnList = "guest_email"),
        @Index(name = "idx_cgv_booking_guest_phone", columnList = "guest_phone")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Booking extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column
    String userId;

    @Column(name = "guest_name", length = 100)
    String guestName;

    @Column(name = "guest_email", length = 100)
    String guestEmail;

    @Column(name = "guest_phone", length = 20)
    String guestPhone;

    @Column(name = "showtime_id", nullable = false)
    UUID showtimeId;

    @Column
    UUID promotionId;

    @Column(nullable = false)
    BigDecimal totalBaseAmount;

    @Builder.Default
    @Column
    BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    BigDecimal finalAmount;

    @Column(name = "payment_deadline")
    Instant paymentDeadline;

    @Column
    String qrCodeUrl;

    @Column
    Instant cancelledAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<BookingSeat> bookingSeats = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    BookingStatus status;
}
