package com.cgv.catalogservice.scheduler;

import com.cgv.catalogservice.enums.ShowingStatus;
import com.cgv.catalogservice.repository.MovieRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j(topic = "MOVIE-STATUS-SCHEDULER")
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MovieStatusScheduler {

    MovieRepository movieRepository;

    private static final ZoneId VIETNAM_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    @Scheduled(
            cron = "0 5 0 * * *",
            zone = "Asia/Ho_Chi_Minh"
    )
    @Caching(
            evict = {
                    @CacheEvict(
                            value = "movie",
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = "nowShowingMovies",
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = "comingSoonMovies",
                            allEntries = true
                    )
            }
    )
    @Transactional
    public void updateExpiredMoviesToEnded() {

        LocalDate today =
                LocalDate.now(VIETNAM_ZONE);

        log.info(
                "Starting expired movie status update: date={}",
                today
        );

        movieRepository.updateExpiredMoviesToEnded(
                today,
                ShowingStatus.ENDED
        );

        log.info(
                "Finished expired movie status update: date={}, targetStatus={}",
                today,
                ShowingStatus.ENDED
        );
    }
}