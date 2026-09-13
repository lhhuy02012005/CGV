package com.cgv.identityservice.entity;

import com.cgv.commondto.entity.BaseEntity;
import com.cgv.identityservice.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(unique = true , nullable = false)
    String email;

    @Column
    String fullName;

    @Column
    String password;

    @Column(name = "keycloak_id", unique = true, nullable = false)
    String keycloakId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    Role role = Role.USER;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_tier", nullable = false)
    MemberShipTier membershipTier;

    @Column(name = "total_spend_ytd")
    BigDecimal total_spend_ytd;

    @Builder.Default
    @Version
    int version = 1;

}
