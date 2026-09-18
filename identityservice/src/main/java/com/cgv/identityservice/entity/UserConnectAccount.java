package com.cgv.identityservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_connected_accounts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cgv_provider_provider_id", columnNames = {"provider", "provider_id"})
}, indexes = {
        @Index(name = "idx_cgv_connected_user_id", columnList = "user_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserConnectAccount extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(nullable = false)
    String provider;

    @Column(name = "provider_id", nullable = false)
    String providerId;

    @Column(name = "provider_email")
    String providerEmail;

    @Column(name = "access_token", columnDefinition = "TEXT")
    String accessToken;

    @Column(name = "token_expires_at")
    Instant tokenExpiresAt;
}
