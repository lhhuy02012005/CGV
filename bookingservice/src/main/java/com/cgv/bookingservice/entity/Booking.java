package com.cgv.bookingservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.cfg.defs.UUIDDef;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bookings" , indexes = {
        @Index(name = "idx_cgv_booking_user_id" , columnList = "user_id"),
        @Index(name = "idx_cgv_booking_showtime_id", columnList = "showtime_id"),
        @Index(name = "idx_cgv_booking_status" , columnList = "status"),
        @Index(name = "idx_cgv_booking_payment_deadline", columnList = "payment_deadline"),
        @Index(name = "idx_cgv_booking_created_at", columnList = "created_at")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Booking extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false)
    String userId;

    @Column(nullable = false)
    String showTimeId;

    @Column
    UUID promotionId;

    @Column(nullable = false)
    BigDecimal totalBaseAmount;

    @Builder.Default
    @Column
    BigDecimal discountAmount = BigDecimal.ZERO;


    @Column(nullable = false)
    BigDecimal finalAmount;

    @Column
    Instant paymentDeadLine;

    @Column
    String qrCodeUrl;

    @Column
    Instant cancelledAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<BookingSeat> bookingSeats = new ArrayList<>();

}
