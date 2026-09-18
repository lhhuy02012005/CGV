package com.cgv.catalogservice.dto.request.movie;

import com.cgv.catalogservice.enums.MovieStatus;
import com.cgv.catalogservice.enums.ShowingStatus;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record MovieFilterRequest(

        String keyword,

        Integer genreId,

        ShowingStatus showingStatus,

        MovieStatus status,

        LocalDate releaseFrom,

        LocalDate releaseTo,

        @Positive(message = "Thời lượng tối thiểu phải lớn hơn 0")
        Integer minDuration,

        @Positive(message = "Thời lượng tối đa phải lớn hơn 0")
        Integer maxDuration,

        String language,

        String ageRating

) {
}
