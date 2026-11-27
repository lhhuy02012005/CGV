package com.cgv.paymentservice.dto;

import com.cgv.paymentservice.enums.PaymentStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReconcileResponse {
    UUID paymentId;
    String transactionId;
    PaymentStatus localStatus;
    BigDecimal localAmount;
    /** vnp_ResponseCode của lệnh querydr (00 = tra cứu thành công). */
    String gatewayResponseCode;
    /** vnp_TransactionStatus: 00 thành công, 01 chưa hoàn tất, 02 lỗi, 05/06 đang hoàn/đã gửi hoàn tiền... */
    String gatewayTransactionStatus;
    BigDecimal gatewayAmount;
    String gatewayTransactionNo;
    String gatewayMessage;
    boolean matched;
    String message;
}
