package com.cgv.catalogservice.entity;

import com.cgv.catalogservice.enums.ArticleCategory;
import com.cgv.commondto.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "articles",
        indexes = {
                @Index(
                        name = "idx_articles_slug",
                        columnList = "slug",
                        unique = true
                ),
                @Index(
                        name = "idx_articles_category",
                        columnList = "category"
                ),
                @Index(
                        name = "idx_articles_is_featured",
                        columnList = "is_featured"
                ),
                @Index(
                        name = "idx_articles_is_trending",
                        columnList = "is_trending"
                ),
                @Index(
                        name = "idx_articles_published_at",
                        columnList = "published_at"
                ),
                @Index(
                        name = "idx_articles_movie_id",
                        columnList = "movie_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Article extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(
            name = "title",
            nullable = false
    )
    String title;

    @Column(
            name = "slug",
            nullable = false,
            unique = true
    )
    String slug;

    @Column(name = "thumbnail_url")
    String thumbnailUrl;

    @Column(
            name = "content",
            nullable = false,
            columnDefinition = "TEXT"
    )
    String content;

    @Column(
            name = "excerpt",
            length = 500
    )
    String excerpt;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "category",
            nullable = false
    )
    ArticleCategory category;

    @Column(
            name = "tags",
            columnDefinition = "TEXT"
    )
    String tags;

    @Column(name = "author_name")
    String authorName;

    @Column(name = "views")
    @Builder.Default
    Integer views = 0;

    @Column(name = "is_featured")
    @Builder.Default
    Boolean featured = false;

    @Column(name = "is_trending")
    @Builder.Default
    Boolean trending = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "movie_id"
    )
    Movie movie;

    @Column(name = "published_at")
    LocalDateTime publishedAt;
}
