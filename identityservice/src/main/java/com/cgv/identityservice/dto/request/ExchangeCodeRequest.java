package com.cgv.identityservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExchangeCodeRequest {
    @NotBlank(message = "Mã xác thực (code) không được để trống")
    String code;

    @NotBlank(message = "Redirect URI không được để trống")
    String redirectUri;
}
