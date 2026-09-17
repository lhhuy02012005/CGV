package com.cgv.identityservice.dto.response;

import com.cgv.identityservice.entity.MemberShipTier;
import com.cgv.identityservice.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    String id;
    String email;
    String fullName;
    MemberShipTierResponse membershipTier;
    BigDecimal total_spend_ytd;
}
