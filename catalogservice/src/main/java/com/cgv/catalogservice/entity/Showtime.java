package com.cgv.catalogservice.entity;

import com.cgv.catalogservice.converter.FormatConverter;
import com.cgv.catalogservice.enums.Format;
import com.cgv.catalogservice.enums.ShowtimeStatus;
import com.cgv.commondto.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "showtimes",
        indexes = {
                @Index(
                        name = "idx_showtimes_movie_id",
                        columnList = "movie_id"
                ),
                @Index(
                        name = "idx_showtimes_show_date",
                        columnList = "show_date"
                ),
                @Index(
                        name = "idx_showtimes_show_date_status",
                        columnList = "show_date, status"
                ),
                @Index(
                        name = "idx_showtimes_room_time",
                        columnList = "room_id,start_time, end_time",
                        unique = true
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Showtime extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "movie_id",
            nullable = false
    )
    Movie movie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "room_id",
            nullable = false
    )
    Room room;

    @Column(
            name = "show_date",
            nullable = false
    )
    LocalDate showDate;

    @Column(
            name = "start_time",
            nullable = false
    )
    LocalDateTime startTime;

    @Column(
            name = "end_time",
            nullable = false
    )
    LocalDateTime endTime;

    @Column(name = "language")
    String language;

    @Column(name = "subtitle_language")
    String subtitleLanguage;

    @Convert(converter = FormatConverter.class)
    @Column(name = "format")
    Format format;

    @Column(
            name = "base_price",
            nullable = false,
            precision = 12,
            scale = 2
    )
    BigDecimal basePrice;

    @Column(name = "available_seats")
    Integer availableSeats;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false
    )
    @Builder.Default
    ShowtimeStatus status = ShowtimeStatus.SCHEDULED;
}
