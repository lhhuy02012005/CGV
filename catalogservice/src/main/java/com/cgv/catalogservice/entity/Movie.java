package com.cgv.catalogservice.entity;

import com.cgv.catalogservice.enums.MovieStatus;
import com.cgv.catalogservice.enums.ShowingStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "movies",
        indexes = {
                @Index(name = "idx_movies_showing_status", columnList = "showing_status"),
                @Index(name = "idx_movies_release_date", columnList = "release_date"),
                @Index(name = "idx_movies_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Movie extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false, unique = true)
    String title;

    @Column(name = "original_title")
    String originalTitle;

    @Column(columnDefinition = "TEXT")
    String synopsis;

    String director;

    String language;

    String subtitle;

    @Column(name = "age_rating", length = 5)
    String ageRating;

    @Column(name = "duration_minutes")
    Integer durationMinutes;

    @Column(name = "release_date")
    LocalDate releaseDate;

    @Column(name = "end_date")
    LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "showing_status", nullable = false)
    ShowingStatus showingStatus;

    @Column(name = "poster_url")
    String posterUrl;

    @Column(name = "backdrop_url")
    String backdropUrl;

    @Column(name = "trailer_youtube_url")
    String trailerYoutubeUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    MovieStatus status = MovieStatus.ACTIVE;
}
