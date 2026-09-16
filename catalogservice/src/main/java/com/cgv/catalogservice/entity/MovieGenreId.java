package com.cgv.catalogservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MovieGenreId implements Serializable {

    @Column(name = "movie_id")
    UUID movieId;

    @Column(name = "genre_id")
    Integer genreId;
}
