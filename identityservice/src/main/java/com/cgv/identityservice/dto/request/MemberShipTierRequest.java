package com.cgv.identityservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MemberShipTierRequest {

    @NotBlank(message = "Mã hạng thành viên không được để trống")
    String code;

    @NotBlank(message = "Tên hạng thành viên không được để trống")
    String name;

    @NotNull(message = "Mức chi tiêu tối thiểu không được để trống")
    @PositiveOrZero(message = "Mức chi tiêu tối thiểu phải lớn hơn hoặc bằng 0")
    BigDecimal minSpend;

    String description;
}
