package com.cgv.catalogservice.entity;

import com.cgv.catalogservice.enums.SeatTypeName;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "seat_types")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeatType {

    @Id
    @Enumerated(EnumType.STRING)
    SeatTypeName name;

    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    @Builder.Default
    BigDecimal surcharge = BigDecimal.valueOf(0);

    @Column(columnDefinition = "TEXT")
    String description;
}
