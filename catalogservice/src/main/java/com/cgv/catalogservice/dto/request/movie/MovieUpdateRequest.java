package com.cgv.catalogservice.dto.request.movie;

import com.cgv.catalogservice.enums.ShowingStatus;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record MovieUpdateRequest(

        String title,

        String originalTitle,

        String synopsis,

        String director,

        String language,

        String subtitle,

        @Size(max = 5, message = "Phân loại độ tuổi tối đa 5 ký tự")
        String ageRating,

        @Positive(message = "Thời lượng phim phải lớn hơn 0")
        Integer durationMinutes,

        LocalDate releaseDate,

        LocalDate endDate,

        ShowingStatus showingStatus,

        String posterUrl,

        String backdropUrl,

        String trailerYoutubeUrl
) {
}