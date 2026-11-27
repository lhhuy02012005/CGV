package com.cgv.catalogservice.listener;

import com.cgv.catalogservice.document.MovieDocument;
import com.cgv.catalogservice.entity.Movie;
import com.cgv.catalogservice.entity.MovieCast;
import com.cgv.catalogservice.repository.MovieCastRepository;
import com.cgv.catalogservice.repository.MovieGenreRepository;
import com.cgv.catalogservice.repository.MovieRepository;
import com.cgv.catalogservice.repository.search.MovieSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j(topic = "ES-SYNC")
@RequiredArgsConstructor
public class MovieElasticsearchSyncListener {

    private final MovieSearchRepository movieSearchRepository;
    private final MovieRepository movieRepository;
    private final MovieCastRepository movieCastRepository;
    private final MovieGenreRepository movieGenreRepository;

    // Chạy ngầm (@Async) sau khi DB đã commit thành công
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMovieSaved(Movie movie) {
        reindex(movie.getId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMovieIndexRequested(MovieIndexEvent event) {
        reindex(event.movieId());
    }

    /**
     * Phim nạp bằng Flyway (seed/demo) không đi qua service nên chưa có trên Elasticsearch:
     * đồng bộ toàn bộ một lần khi service khởi động.
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void reindexAllOnStartup() {
        try {
            List<UUID> ids = movieRepository.findAll().stream().map(Movie::getId).toList();
            ids.forEach(this::reindex);
            log.info("Đã đồng bộ {} phim lên Elasticsearch khi khởi động", ids.size());
        } catch (Exception e) {
            log.error("Lỗi đồng bộ toàn bộ phim lên Elasticsearch (tìm kiếm sẽ dùng SQL dự phòng): {}", e.getMessage());
        }
    }

    private void reindex(UUID movieId) {
        try {
            Movie movie = movieRepository.findById(movieId).orElse(null);
            if (movie == null) {
                movieSearchRepository.deleteById(movieId.toString());
                return;
            }

            List<String> casts = movieCastRepository.findByMovieIdOrderByDisplayOrderAsc(movieId).stream()
                    .map(MovieCast::getActorName)
                    .filter(name -> name != null && !name.isBlank())
                    .toList();
            List<String> genres = movieGenreRepository.findByIdMovieId(movieId).stream()
                    .map(mg -> mg.getGenre().getName())
                    .toList();

            MovieDocument doc = MovieDocument.builder()
                    .id(movie.getId().toString())
                    .title(movie.getTitle())
                    .originalTitle(movie.getOriginalTitle())
                    .synopsis(movie.getSynopsis())
                    .director(movie.getDirector())
                    .casts(casts)
                    .genres(genres)
                    .country(movie.getCountry())
                    .ageRating(movie.getAgeRating())
                    .showingStatus(movie.getShowingStatus() == null ? null : movie.getShowingStatus().name())
                    .releaseDate(movie.getReleaseDate())
                    .build();

            movieSearchRepository.save(doc);
            log.info("Đã đồng bộ phim '{}' lên Elasticsearch thành công!", movie.getTitle());
        } catch (Exception e) {
            log.error("Lỗi khi đồng bộ phim {} lên Elasticsearch (DB vẫn an toàn): {}", movieId, e.getMessage());
        }
    }
}
