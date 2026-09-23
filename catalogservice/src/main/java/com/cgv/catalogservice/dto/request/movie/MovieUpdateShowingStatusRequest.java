package com.cgv.catalogservice.dto.request.movie;

import com.cgv.catalogservice.enums.ShowingStatus;
import jakarta.validation.constraints.NotNull;

public record MovieUpdateShowingStatusRequest(

        @NotNull(message = "Trạng thái chiếu không được để trống")
        ShowingStatus showingStatus
) {
}
