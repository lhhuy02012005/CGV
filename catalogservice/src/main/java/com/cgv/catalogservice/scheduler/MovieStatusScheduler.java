package com.cgv.catalogservice.scheduler;

import com.cgv.catalogservice.enums.ShowingStatus;
import com.cgv.catalogservice.repository.MovieRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

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
    @Transactional
    public void updateExpiredMoviesToEnded() {

        LocalDate today =
                LocalDate.now(VIETNAM_ZONE);

        movieRepository.updateExpiredMoviesToEnded(
                today,
                ShowingStatus.ENDED
        );
    }
}