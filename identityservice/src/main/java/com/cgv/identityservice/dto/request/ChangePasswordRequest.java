package com.cgv.identityservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangePasswordRequest {
    @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại")
    String currentPassword;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới")
    @Size(min = 6, max = 64, message = "Mật khẩu mới phải từ 6 đến 64 ký tự")
    String newPassword;
}
