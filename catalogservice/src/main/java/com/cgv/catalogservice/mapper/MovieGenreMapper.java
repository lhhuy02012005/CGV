package com.cgv.catalogservice.mapper;

import com.cgv.catalogservice.dto.request.moviegenre.MovieGenreCreateRequest;
import com.cgv.catalogservice.dto.response.MovieGenreResponse;
import com.cgv.catalogservice.entity.Genre;
import com.cgv.catalogservice.entity.Movie;
import com.cgv.catalogservice.entity.MovieGenre;
import com.cgv.catalogservice.entity.MovieGenreId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

@Mapper(config = CatalogMapperConfig.class)
public interface MovieGenreMapper {

    default MovieGenre toEntity(
            MovieGenreCreateRequest request,
            Movie movie,
            Genre genre
    ) {
        if (request == null) {
            return null;
        }

        UUID movieId = movie != null ? movie.getId() : request.movieId();
        Integer genreId = genre != null ? genre.getId() : request.genreId();

        MovieGenre movieGenre = new MovieGenre();
        movieGenre.setId(new MovieGenreId(movieId, genreId));
        movieGenre.setMovie(movie);
        movieGenre.setGenre(genre);
        return movieGenre;
    }

    @Mapping(target = "movieId", source = "id.movieId")
    @Mapping(target = "movieTitle", source = "movie.title")
    @Mapping(target = "genreId", source = "id.genreId")
    @Mapping(target = "genreName", source = "genre.name")
    @Mapping(target = "genreSlug", source = "genre.slug")
    MovieGenreResponse toResponse(MovieGenre movieGenre);

    List<MovieGenreResponse> toResponseList(List<MovieGenre> movieGenres);
}
