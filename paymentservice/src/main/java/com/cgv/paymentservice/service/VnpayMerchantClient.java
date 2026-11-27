package com.cgv.paymentservice.service;

import com.cgv.commondto.exception.BusinessException;
import com.cgv.commondto.exception.ErrorCode;
import com.cgv.paymentservice.config.VnpayConfig;
import com.cgv.paymentservice.entity.Payment;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gọi Merchant API của VNPay (sandbox): tra cứu giao dịch (querydr) và hoàn tiền (refund).
 * Chuỗi ký HMAC-SHA512 nối các trường bằng dấu "|" theo thứ tự trong tài liệu VNPay 2.1.0.
 */
@Component
@Slf4j(topic = "VNPAY-MERCHANT")
public class VnpayMerchantClient {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNP_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(VN);
    private static final String TRANS_NO_PREFIX = "VNPAY_TRANS_NO:";

    private final VnpayConfig vnpayConfig;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public VnpayMerchantClient(VnpayConfig vnpayConfig, ObjectMapper objectMapper) {
        this.vnpayConfig = vnpayConfig;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(15_000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /** Phản hồi từ VNPay (đã kiểm tra chữ ký nếu VNPay gửi kèm). */
    public record GatewayResult(Map<String, String> fields, String requestJson, String responseJson,
                                boolean signatureValid) {
        public String get(String key) {
            return fields.getOrDefault(key, "");
        }
    }

    public GatewayResult queryTransaction(Payment payment, String ipAddress) {
        String requestId = newRequestId();
        String createDate = VNP_DATE.format(Instant.now());
        String txnRef = payment.getTransactionId();
        String transactionDate = transactionDate(payment);
        String orderInfo = "Doi soat giao dich " + txnRef;
        String ip = safeIp(ipAddress);
        String version = vnpayConfig.getVnpVersion();
        String tmnCode = vnpayConfig.getVnpTmnCode();

        Map<String, String> body = new LinkedHashMap<>();
        body.put("vnp_RequestId", requestId);
        body.put("vnp_Version", version);
        body.put("vnp_Command", "querydr");
        body.put("vnp_TmnCode", tmnCode);
        body.put("vnp_TxnRef", txnRef);
        body.put("vnp_OrderInfo", orderInfo);
        body.put("vnp_TransactionDate", transactionDate);
        body.put("vnp_CreateDate", createDate);
        body.put("vnp_IpAddr", ip);
        body.put("vnp_SecureHash", sign(String.join("|", requestId, version, "querydr",
                tmnCode, txnRef, transactionDate, createDate, ip, orderInfo)));
        return post(body);
    }

    public GatewayResult refund(Payment payment, String createdBy, String ipAddress) {
        String requestId = newRequestId();
        String createDate = VNP_DATE.format(Instant.now());
        String txnRef = payment.getTransactionId();
        String amount = String.valueOf(payment.getAmount().movePointRight(2).longValueExact());
        String transactionNo = gatewayTransactionNo(payment);
        String transactionDate = transactionDate(payment);
        String orderInfo = "Hoan tien don " + payment.getBookingId();
        String ip = safeIp(ipAddress);
        String transactionType = "02"; // hoàn toàn phần
        String version = vnpayConfig.getVnpVersion();
        String tmnCode = vnpayConfig.getVnpTmnCode();

        Map<String, String> body = new LinkedHashMap<>();
        body.put("vnp_RequestId", requestId);
        body.put("vnp_Version", version);
        body.put("vnp_Command", "refund");
        body.put("vnp_TmnCode", tmnCode);
        body.put("vnp_TransactionType", transactionType);
        body.put("vnp_TxnRef", txnRef);
        body.put("vnp_Amount", amount);
        body.put("vnp_OrderInfo", orderInfo);
        body.put("vnp_TransactionNo", transactionNo);
        body.put("vnp_TransactionDate", transactionDate);
        body.put("vnp_CreateBy", createdBy);
        body.put("vnp_CreateDate", createDate);
        body.put("vnp_IpAddr", ip);
        body.put("vnp_SecureHash", sign(String.join("|", requestId, version, "refund",
                tmnCode, transactionType, txnRef, amount, transactionNo, transactionDate,
                createdBy, createDate, ip, orderInfo)));
        return post(body);
    }

    /** Mã giao dịch phía VNPay được lưu ở cột payload sau callback: "VNPAY_TRANS_NO:<số>". */
    public static String gatewayTransactionNo(Payment payment) {
        String raw = payment.getRaw();
        if (raw != null && raw.startsWith(TRANS_NO_PREFIX)) {
            String value = raw.substring(TRANS_NO_PREFIX.length()).trim();
            return "null".equals(value) ? "" : value;
        }
        return "";
    }

    /** vnp_TxnRef = CGV_<bookingId>_<epochMillis>, sinh cùng lúc với vnp_CreateDate của giao dịch gốc. */
    static String transactionDate(Payment payment) {
        String txnRef = payment.getTransactionId();
        int idx = txnRef == null ? -1 : txnRef.lastIndexOf('_');
        if (idx > 0) {
            try {
                return VNP_DATE.format(Instant.ofEpochMilli(Long.parseLong(txnRef.substring(idx + 1))));
            } catch (NumberFormatException ignored) {
                // dùng createdAt bên dưới
            }
        }
        return VNP_DATE.format(payment.getCreatedAt() == null ? Instant.now() : payment.getCreatedAt());
    }

    private GatewayResult post(Map<String, String> body) {
        String requestJson;
        String responseJson;
        try {
            requestJson = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION, "Không tạo được yêu cầu gửi VNPay");
        }
        try {
            responseJson = restClient.post()
                    .uri(vnpayConfig.getVnpApiUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestJson)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            log.error("Không gọi được VNPay merchant API: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_OPERATION, "Không kết nối được cổng VNPay, vui lòng thử lại sau");
        }

        Map<String, String> fields = new LinkedHashMap<>();
        try {
            Map<String, Object> raw = objectMapper.readValue(responseJson == null ? "{}" : responseJson,
                    new TypeReference<Map<String, Object>>() {});
            raw.forEach((k, v) -> fields.put(k, v == null ? "" : String.valueOf(v)));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION, "Phản hồi VNPay không hợp lệ");
        }

        return new GatewayResult(fields, requestJson, responseJson, verifyResponseSignature(fields));
    }

    /** true nếu chữ ký hợp lệ hoặc VNPay không ký phản hồi (một số mã lỗi định dạng như 99). */
    private boolean verifyResponseSignature(Map<String, String> f) {
        String received = f.get("vnp_SecureHash");
        if (received == null || received.isBlank()) {
            return !"00".equals(f.get("vnp_ResponseCode"));
        }
        String command = v(f, "vnp_Command");
        String data;
        if ("querydr".equals(command)) {
            data = String.join("|", v(f, "vnp_ResponseId"), command, v(f, "vnp_ResponseCode"), v(f, "vnp_Message"),
                    v(f, "vnp_TmnCode"), v(f, "vnp_TxnRef"), v(f, "vnp_Amount"), v(f, "vnp_BankCode"),
                    v(f, "vnp_PayDate"), v(f, "vnp_TransactionNo"), v(f, "vnp_TransactionType"),
                    v(f, "vnp_TransactionStatus"), v(f, "vnp_OrderInfo"), v(f, "vnp_PromotionCode"),
                    v(f, "vnp_PromotionAmount"));
        } else if ("refund".equals(command)) {
            data = String.join("|", v(f, "vnp_ResponseId"), command, v(f, "vnp_ResponseCode"), v(f, "vnp_Message"),
                    v(f, "vnp_TmnCode"), v(f, "vnp_TxnRef"), v(f, "vnp_Amount"), v(f, "vnp_BankCode"),
                    v(f, "vnp_PayDate"), v(f, "vnp_TransactionNo"), v(f, "vnp_TransactionType"),
                    v(f, "vnp_TransactionStatus"), v(f, "vnp_OrderInfo"));
        } else {
            return false;
        }
        boolean valid = MessageDigest.isEqual(sign(data).getBytes(StandardCharsets.UTF_8),
                received.toLowerCase().getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            log.error("Chữ ký phản hồi VNPay không hợp lệ cho lệnh {} txn={}", command, v(f, "vnp_TxnRef"));
        }
        return valid;
    }

    private static String v(Map<String, String> f, String key) {
        return f.getOrDefault(key, "");
    }

    private String sign(String data) {
        return vnpayConfig.hmacSHA512(vnpayConfig.getSecretKey(), data);
    }

    private static String newRequestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String safeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "127.0.0.1";
        }
        String first = ip.split(",")[0].trim();
        return first.length() > 45 ? first.substring(0, 45) : first;
    }
}
