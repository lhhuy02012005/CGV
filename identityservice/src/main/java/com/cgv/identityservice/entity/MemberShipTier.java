package com.cgv.identityservice.entity;

import com.cgv.commondto.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "membership_tiers")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MemberShipTier extends BaseEntity {
    @Id
    @Column(unique = true)
    String code;

    @Column
    String name;

    @Builder.Default
    @Column
    BigDecimal minSpend  = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    String description;

}
