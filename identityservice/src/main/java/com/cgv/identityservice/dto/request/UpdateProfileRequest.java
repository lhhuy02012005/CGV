package com.cgv.identityservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateProfileRequest {
    @NotBlank(message = "Họ và tên không được để trống")
    String fullName;

    String phone;

    @jakarta.validation.constraints.Past(message = "Ngày sinh phải ở trong quá khứ")
    java.time.LocalDate birthDate;

    @jakarta.validation.constraints.Pattern(regexp = "MALE|FEMALE|OTHER", message = "Giới tính không hợp lệ")
    String gender;

    @jakarta.validation.constraints.Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    String address;
}
