package com.cgv.catalogservice.entity;

import com.cgv.catalogservice.enums.RoleType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Table(name = "movie_casts",
        indexes = {
                @Index(name = "idx_movie_casts_movie_id", columnList = "movie_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MovieCast {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    Movie movie;

    @Column(name = "actor_name", nullable = false)
    String actorName;

    @Column(name = "character_name")
    String characterName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type")
    RoleType roleType;

    @Column(name = "avatar_url")
    String avatarUrl;

    @Column(name = "display_order")
    @Builder.Default
    Integer displayOrder = 0;
}
