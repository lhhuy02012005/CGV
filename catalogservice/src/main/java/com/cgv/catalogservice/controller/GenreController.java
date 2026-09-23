package com.cgv.catalogservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import com.cgv.catalogservice.dto.request.genre.GenreCreateRequest;
import com.cgv.catalogservice.dto.request.genre.GenreUpdateRequest;
import com.cgv.catalogservice.dto.response.GenreResponse;
import com.cgv.catalogservice.service.GenreService;
import com.cgv.commondto.dto.ApiResponse;
import com.cgv.commondto.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Genres", description = "API quản lý thể loại phim.")
@RestController
@RequestMapping("/genres")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GenreController {

    GenreService genreService;

    @Operation(
            summary = "Tạo thể loại",
            description = "Tạo thể loại phim mới. Slug được sinh tự động từ tên ở tầng service."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GenreResponse> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu tạo thể loại", required = true) @RequestBody @Valid GenreCreateRequest request
    ) {
        GenreResponse response = genreService.createGenre(request);

        return ApiResponse.<GenreResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(response)
                .message("Tạo thể loại thành công")
                .build();
    }

    @Operation(
            summary = "Cập nhật thể loại",
            description = "Thay thế thông tin tên của thể loại theo ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PutMapping("/{genreId}")
    public ApiResponse<GenreResponse> update(
            @Parameter(description = "ID của thể loại", required = true, schema = @Schema(type = "integer", format = "int32")) @PathVariable Integer genreId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu cập nhật thể loại", required = true) @RequestBody @Valid GenreUpdateRequest request
    ) {
        GenreResponse response = genreService.updateGenre(genreId, request);

        return ApiResponse.<GenreResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Cập nhật thể loại thành công")
                .build();
    }

    @Operation(
            summary = "Xoá thể loại",
            description = "Xoá thể loại nếu không còn được phim sử dụng."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên cần xoá"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Không thể xoá do tài nguyên đang được tham chiếu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @DeleteMapping("/{genreId}")
    public ApiResponse<Void> delete(@Parameter(description = "ID của thể loại", required = true, schema = @Schema(type = "integer", format = "int32")) @PathVariable Integer genreId) {
        genreService.deleteGenre(genreId);

        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xoá thể loại thành công")
                .build();
    }

    @Operation(
            summary = "Xem chi tiết thể loại",
            description = "Lấy thông tin thể loại theo ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên")
    })
    @GetMapping("/{genreId}")
    public ApiResponse<GenreResponse> get(@Parameter(description = "ID của thể loại", required = true, schema = @Schema(type = "integer", format = "int32")) @PathVariable Integer genreId) {
        GenreResponse response = genreService.getGenreById(genreId);

        return ApiResponse.<GenreResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Xem chi tiết thể loại thành công")
                .build();
    }

    @Operation(
            summary = "Lấy danh sách thể loại",
            description = "Lấy danh sách thể loại có phân trang và sắp xếp."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tham số truy vấn không hợp lệ")
    })
    @GetMapping
    public ApiResponse<PageResponse<GenreResponse>> findAll(
            @ParameterObject @PageableDefault Pageable pageable
    ) {
        PageResponse<GenreResponse> response = genreService.getAllGenres(pageable);

        return ApiResponse.<PageResponse<GenreResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách thể loại")
                .build();
    }
}
