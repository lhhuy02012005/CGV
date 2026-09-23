package com.cgv.catalogservice.repository;

import com.cgv.catalogservice.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ArticleRepository
        extends JpaRepository<Article, UUID>,
        JpaSpecificationExecutor<Article> {

    Optional<Article> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(
            String slug,
            UUID articleId
    );
}
