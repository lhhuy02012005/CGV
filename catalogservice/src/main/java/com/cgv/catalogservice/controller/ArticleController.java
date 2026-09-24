package com.cgv.catalogservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import com.cgv.catalogservice.dto.request.article.ArticleCreateRequest;
import com.cgv.catalogservice.dto.request.article.ArticleFilterRequest;
import com.cgv.catalogservice.dto.request.article.ArticleUpdateRequest;
import com.cgv.catalogservice.dto.response.ArticleResponse;
import com.cgv.catalogservice.enums.ArticleCategory;
import com.cgv.catalogservice.service.ArticleService;
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

import java.util.UUID;

@Tag(name = "Articles", description = "API quản lý bài viết, tin tức, bài nổi bật, bài thịnh hành và truy vấn theo danh mục.")
@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ArticleController {

    ArticleService articleService;

    @Operation(
            summary = "Tạo bài viết",
            description = "Tạo bài viết mới. Slug được sinh tự động từ tiêu đề ở tầng service."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ArticleResponse> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu tạo bài viết", required = true) @RequestBody @Valid ArticleCreateRequest request
    ) {

        ArticleResponse response =
                articleService.createArticle(request);

        return ApiResponse.<ArticleResponse>builder()
                .status(HttpStatus.CREATED.value())
                .data(response)
                .message("Tạo bài viết thành công")
                .build();
    }

    @Operation(
            summary = "Cập nhật bài viết",
            description = "Cập nhật một phần thông tin bài viết theo ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên liên quan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @PatchMapping("/{articleId}")
    public ApiResponse<ArticleResponse> update(
            @Parameter(description = "ID của bài viết", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID articleId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Dữ liệu cập nhật bài viết", required = true) @RequestBody @Valid ArticleUpdateRequest request
    ) {

        ArticleResponse response =
                articleService.updateArticle(articleId, request);

        return ApiResponse.<ArticleResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Cập nhật bài viết thành công")
                .build();
    }

    @Operation(
            summary = "Xoá bài viết",
            description = "Xoá bài viết theo ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên cần xoá"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Không thể xoá do tài nguyên đang được tham chiếu hoặc vi phạm quy tắc nghiệp vụ")
    })
    @DeleteMapping("/{articleId}")
    public ApiResponse<Void> delete(
            @Parameter(description = "ID của bài viết", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID articleId
    ) {

        articleService.deleteArticle(articleId);

        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xoá bài viết thành công")
                .build();
    }

    @Operation(
            summary = "Xem chi tiết bài viết",
            description = "Lấy chi tiết bài viết theo ID. Mỗi lần đọc có thể làm tăng lượt xem theo logic service."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên")
    })
    @GetMapping("/{articleId}")
    public ApiResponse<ArticleResponse> get(
            @Parameter(description = "ID của bài viết", required = true, schema = @Schema(type = "string", format = "uuid")) @PathVariable UUID articleId
    ) {

        ArticleResponse response =
                articleService.getArticleById(articleId);

        return ApiResponse.<ArticleResponse>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Xem chi tiết bài viết thành công")
                .build();
    }

    @Operation(
            summary = "Lấy bài viết theo danh mục",
            description = "Lấy danh sách bài viết thuộc một danh mục, có phân trang và sắp xếp."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tham số truy vấn không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy tài nguyên cha được yêu cầu")
    })
    @GetMapping("/category/{category}")
    public ApiResponse<PageResponse<ArticleResponse>> getArticlesByCategory(
            @Parameter(description = "Danh mục bài viết", required = true, schema = @Schema(implementation = ArticleCategory.class)) @PathVariable ArticleCategory category,
            @ParameterObject @PageableDefault Pageable pageable
    ) {

        PageResponse<ArticleResponse> response =
                articleService.getArticlesByCategory(category, pageable);

        return ApiResponse.<PageResponse<ArticleResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách bài viết theo loại")
                .build();
    }

    @Operation(
            summary = "Lấy bài viết nổi bật",
            description = "Lấy danh sách bài viết được đánh dấu nổi bật, có phân trang và sắp xếp."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tham số truy vấn không hợp lệ")
    })
    @GetMapping("/featured")
    public ApiResponse<PageResponse<ArticleResponse>> getFeaturedArticles(
            @ParameterObject @PageableDefault Pageable pageable
    ) {

        PageResponse<ArticleResponse> response =
                articleService.getFeaturedArticles(pageable);

        return ApiResponse.<PageResponse<ArticleResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách bài viết nổi bật")
                .build();
    }

    @Operation(
            summary = "Lấy bài viết thịnh hành",
            description = "Lấy danh sách bài viết được đánh dấu thịnh hành, có phân trang và sắp xếp."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tham số truy vấn không hợp lệ")
    })
    @GetMapping("/trending")
    public ApiResponse<PageResponse<ArticleResponse>> getTrendingArticles(
            @ParameterObject @PageableDefault Pageable pageable
    ) {

        PageResponse<ArticleResponse> response =
                articleService.getTrendingArticles(pageable);

        return ApiResponse.<PageResponse<ArticleResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách bài viết thịnh hành")
                .build();
    }

    @Operation(
            summary = "Tìm kiếm và lọc bài viết",
            description = "Lấy danh sách bài viết theo các điều kiện lọc trong ArticleFilterRequest, kèm phân trang và sắp xếp."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tham số truy vấn không hợp lệ")
    })
    @GetMapping
    public ApiResponse<PageResponse<ArticleResponse>> findAll(
            @ParameterObject @Valid @ModelAttribute ArticleFilterRequest filter,
            @ParameterObject @PageableDefault Pageable pageable
    ) {

        PageResponse<ArticleResponse> response =
                articleService.getAllArticles(filter, pageable);

        return ApiResponse.<PageResponse<ArticleResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách bài viết")
                .build();
    }
}