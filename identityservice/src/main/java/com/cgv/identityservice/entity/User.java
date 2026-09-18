package com.cgv.identityservice.entity;

import com.cgv.identityservice.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "users" , indexes = {
        @Index(name = "idx_cgv_user_email" , columnList = "email"),
        @Index(name = "idx_cgv_user_membership_tier" , columnList = "membership_tier")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User extends BaseEntity {
    @Id
    String id;

    @Column(nullable = false, unique = true)
    String email;

    @Column
    String fullName;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_tier", nullable = false)
    MemberShipTier membershipTier;

    @Builder.Default
    @Column(name = "total_spend_ytd", nullable = false)
    BigDecimal total_spend_ytd = BigDecimal.ZERO;

    @Builder.Default
    @Version
    int version = 1;

}
