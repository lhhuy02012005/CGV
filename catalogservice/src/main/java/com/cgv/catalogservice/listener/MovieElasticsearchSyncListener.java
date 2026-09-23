package com.cgv.catalogservice.listener;

import com.cgv.catalogservice.document.MovieDocument;
import com.cgv.catalogservice.entity.Movie;
import com.cgv.catalogservice.repository.search.MovieSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j(topic = "ES-SYNC")
@RequiredArgsConstructor
public class MovieElasticsearchSyncListener {

    private final MovieSearchRepository movieSearchRepository;

    // Chạy ngầm (@Async) sau khi DB đã commit thành công
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMovieSaved(Movie movie) {
        try {
            MovieDocument doc = MovieDocument.builder()
                    .id(movie.getId().toString())
                    .title(movie.getTitle())
                    .originalTitle(movie.getOriginalTitle())
                    .synopsis(movie.getSynopsis())
                    .director(movie.getDirector())
                    .ageRating(movie.getAgeRating())
                    .showingStatus(movie.getShowingStatus().name())
                    .releaseDate(movie.getReleaseDate())
                    .build();

            movieSearchRepository.save(doc);
            log.info("Đã đồng bộ phim '{}' lên Elasticsearch thành công!", movie.getTitle());
        } catch (Exception e) {
            log.error("Lỗi khi đồng bộ phim lên Elasticsearch (DB vẫn an toàn): ", e);
        }
    }
}