package com.cgv.catalogservice.entity;

import com.cgv.catalogservice.enums.EventStatus;
import com.cgv.commondto.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
        name = "events",
        indexes = {
                @Index(
                        name = "idx_events_event_date",
                        columnList = "event_date"
                ),
                @Index(
                        name = "idx_events_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_events_cinema_id",
                        columnList = "cinema_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Event extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(
            name = "title",
            nullable = false
    )
    String title;

    @Column(
            name = "description",
            columnDefinition = "TEXT"
    )
    String description;

    @Column(name = "thumbnail_url")
    String thumbnailUrl;

    @Column(
            name = "event_date",
            nullable = false
    )
    LocalDate eventDate;

    @Column(name = "event_time")
    LocalTime eventTime;

    @Column(name = "location_name")
    String locationName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "cinema_id"
    )
    Cinema cinema;

    @Column(name = "registration_url")
    String registrationUrl;

    @Column(name = "max_attendees")
    Integer maxAttendees;

    @Column(name = "current_attendees")
    @Builder.Default
    Integer currentAttendees = 0;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false
    )
    @Builder.Default
    EventStatus status = EventStatus.UPCOMING;
}
