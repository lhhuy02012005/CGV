package com.cgv.catalogservice.mapper;

import com.cgv.catalogservice.dto.request.article.ArticleCreateRequest;
import com.cgv.catalogservice.dto.request.article.ArticleUpdateRequest;
import com.cgv.catalogservice.dto.response.ArticleResponse;
import com.cgv.catalogservice.entity.Article;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = CatalogMapperConfig.class)
public interface ArticleMapper {

    @Mapping(target = "movie", ignore = true)
    @Mapping(target = "views", ignore = true)
    Article toEntity(ArticleCreateRequest request);

    @Mapping(target = "movie", ignore = true)
    @Mapping(target = "views", ignore = true)
    void updateEntity(ArticleUpdateRequest request, @MappingTarget Article article);

    @Mapping(target = "movieId", source = "movie.id")
    @Mapping(target = "movieTitle", source = "movie.title")
    ArticleResponse toResponse(Article article);

    List<ArticleResponse> toResponseList(List<Article> articles);
}
