package com.cgv.catalogservice.scheduler;

import com.cgv.catalogservice.enums.ShowingStatus;
import com.cgv.catalogservice.repository.MovieRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
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

    // Chạy lúc 00:05 hằng ngày và một lần khi service khởi động: nếu stack bật sau 00:05
    // (hoặc dữ liệu được nạp bằng Flyway), trạng thái chiếu không bị lệch tới hết ngày.
    @Scheduled(
            cron = "0 5 0 * * *",
            zone = "Asia/Ho_Chi_Minh"
    )
    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE) // xong trước khi index lại Elasticsearch lúc khởi động
    @Caching(
            evict = {
                    @CacheEvict(
                            value = "movies:detail",
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = "movies:now-showing",
                            allEntries = true
                    ),
                    @CacheEvict(
                            value = "movies:coming-soon",
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

        movieRepository.synchronizeShowingStatuses(
                today,
                ShowingStatus.ENDED,
                ShowingStatus.COMING_SOON,
                ShowingStatus.NOW_SHOWING
        );

        log.info(
                "Finished expired movie status update: date={}, targetStatus={}",
                today,
                ShowingStatus.ENDED
        );
    }
}
