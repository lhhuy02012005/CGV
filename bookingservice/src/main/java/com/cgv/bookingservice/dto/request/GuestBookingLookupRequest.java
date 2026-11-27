package com.cgv.bookingservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Tra cứu vé không cần đăng nhập: bắt buộc mã đơn + (email hoặc số điện thoại) khớp với đơn,
 * tránh việc chỉ cần biết email/SĐT là xem được toàn bộ vé của người khác.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GuestBookingLookupRequest {
    @NotBlank(message = "Vui lòng nhập mã đơn vé")
    @Size(max = 100)
    String bookingId;

    @Size(max = 100)
    String email;

    @Size(max = 20)
    String phone;
}
