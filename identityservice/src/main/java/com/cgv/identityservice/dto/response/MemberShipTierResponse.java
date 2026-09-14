package com.cgv.identityservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MemberShipTierResponse {
    String code;
    String name;
    BigDecimal minSpend;
    String description;
}