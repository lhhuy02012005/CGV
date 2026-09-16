package com.cgv.catalogservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(
        name = "movie_genres",
        indexes = {
                @Index(
                        name = "idx_movie_genres_genre_id",
                        columnList = "genre_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MovieGenre {

    @EmbeddedId
    MovieGenreId id;

    @MapsId("movieId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "movie_id",
            nullable = false
    )
    Movie movie;

    @MapsId("genreId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "genre_id",
            nullable = false
    )
    Genre genre;
}
