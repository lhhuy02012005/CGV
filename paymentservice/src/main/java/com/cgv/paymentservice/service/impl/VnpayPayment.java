package com.cgv.paymentservice.service.impl;

import com.cgv.commondto.event.PaymentCompletedEvent;
import com.cgv.commondto.event.PaymentFailedEvent;
import com.cgv.commondto.exception.BusinessException;
import com.cgv.commondto.exception.ErrorCode;
import com.cgv.paymentservice.config.VnpayConfig;
import com.cgv.paymentservice.dto.PaymentStatusResponse;
import com.cgv.paymentservice.entity.OutboxEvent;
import com.cgv.paymentservice.entity.Payment;
import com.cgv.paymentservice.entity.PaymentLog;
import com.cgv.paymentservice.enums.PaymentStatus;
import com.cgv.paymentservice.repository.OutboxEventRepository;
import com.cgv.paymentservice.repository.PaymentRepository;
import com.cgv.paymentservice.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j(topic = "VNPAY")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE , makeFinal = true)
public class VnpayPayment implements PaymentService {
    VnpayConfig vnpayConfig;
    ObjectMapper objectMapper;
    PaymentRepository paymentRepository;
    KafkaTemplate<String, Object> kafkaTemplate;
    OutboxEventRepository outboxEventRepository;
    RedisTemplate<String , Object> redisTemplate;

    int PAYMENT_DEADLINE_DURATION = 10;
    String PAYMENT_TOPIC = "payment.completed";
    @Override
    public String add(HttpServletRequest request , UUID bookingId, BigDecimal amount, String userId) {
       try {
           // VNPay yêu cầu nhân 100
           long vnpAmount = amount.longValue() * 100;
           String vnp_TmnCode = vnpayConfig.getVnpTmnCode();

           String vnp_TxnRef = "CGV_" + bookingId + "_" + System.currentTimeMillis();

           log.info("vnp_ReturnUrl : {} ", vnpayConfig.getVnpReturnUrl());
           Map<String, String> vnp_Params = new HashMap<>();
           vnp_Params.put("vnp_Version", vnpayConfig.getVnpVersion());
           vnp_Params.put("vnp_Command", vnpayConfig.getVnpCommand());
           vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
           vnp_Params.put("vnp_Amount", String.valueOf(vnpAmount));
           vnp_Params.put("vnp_CurrCode", "VND");
           vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
           vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang:" + vnp_TxnRef);
           vnp_Params.put("vnp_Locale", "vn");
           vnp_Params.put("vnp_IpAddr", VnpayConfig.getIpAddress(request)); // BẮT BUỘC
           vnp_Params.put("vnp_OrderType", vnpayConfig.getOrderType());
           vnp_Params.put("vnp_ReturnUrl", vnpayConfig.getVnpReturnUrl());

           Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
           SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
           String createDate = formatter.format(cld.getTime());
           vnp_Params.put("vnp_CreateDate", createDate);

           cld.add(Calendar.MINUTE, PAYMENT_DEADLINE_DURATION);
           vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

           // ====== Build Data ======
           List fieldNames = new ArrayList(vnp_Params.keySet());
           Collections.sort(fieldNames);
           StringBuilder hashData = new StringBuilder();
           StringBuilder query = new StringBuilder();
           Iterator itr = fieldNames.iterator();
           while (itr.hasNext()) {
               String fieldName = (String) itr.next();
               String fieldValue = (String) vnp_Params.get(fieldName);
               if ((fieldValue != null) && (fieldValue.length() > 0)) {
                   //Build hash data
                   hashData.append(fieldName);
                   hashData.append('=');
                   hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                   //Build query
                   query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()));
                   query.append('=');
                   query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                   if (itr.hasNext()) {
                       query.append('&');
                       hashData.append('&');
                   }
               }
           }

// ====== Sinh mã HMAC SHA512 ======
           String queryUrl = query.toString();
           String vnp_SecureHash = vnpayConfig.hmacSHA512(vnpayConfig.getSecretKey(), hashData.toString());
           queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

// ====== Kết quả URL ======
           String paymentUrl = vnpayConfig.getVnpPayUrl() + "?" + queryUrl;
           log.info("VNPay URL generated: {}", paymentUrl);

           redisTemplate.opsForValue().set(vnp_TxnRef, bookingId.toString(), 15, TimeUnit.MINUTES);
           // Tạo bản ghi Payment ở trạng thái PENDING trong Database
           Payment payment = Payment.builder()
                   .bookingId(bookingId)
                   .userId(userId)
                   .amount(amount)
                   .status(PaymentStatus.PENDING)
                   .provider("VNPAY")
                   .transactionId(vnp_TxnRef)
                   .raw(paymentUrl)
                   .build();
           paymentRepository.save(payment);
           return paymentUrl;
       }catch (Exception e){
           log.error("Lỗi khi sinh URL thanh toán VNPAY: ", e);
           throw new BusinessException(ErrorCode.BAD_REQUEST, "Không thể khởi tạo cổng thanh toán VNPAY!");
       }
    }

    public boolean validateVnpayCallback(HttpServletRequest request){
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        String signValue = vnpayConfig.hashAllFields(fields);
        return signValue.equals(vnp_SecureHash);
    }

    @Override
    public boolean callback(HttpServletRequest request){
        Map<String, String[]> params = request.getParameterMap();
        String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
        String vnp_TxnRef = request.getParameter("vnp_TxnRef");
        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        String vnp_BankCode = request.getParameter("vnp_BankCode");
        String vnp_TransactionNo = request.getParameter("vnp_TransactionNo");

        log.info("VNPay return: vnp_ResponseCode={}, vnp_TxnRef={}, vnp_SecureHash={}",
                vnp_ResponseCode, vnp_TxnRef, vnp_SecureHash);


        boolean validSignature = validateVnpayCallback(request);

        if (!validSignature) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Chữ ký VNPAY không hợp lệ!");
        }

        Payment payment = paymentRepository.findByTransactionId(vnp_TxnRef)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "Không tìm thấy giao dịch: " + vnp_TxnRef));
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.warn("Giao dịch {} đã được xử lý trước đó.", vnp_TxnRef);
            return true;
        }

        boolean isSuccess = "00".equals(vnp_ResponseCode);
        if(isSuccess){
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(Instant.now());
            payment.setPaymentMethod("VNPAY_" + vnp_BankCode);
            payment.setRaw("VNPAY_TRANS_NO:" + vnp_TransactionNo);
            redisTemplate.delete(vnp_TxnRef);
            log.info("🎉 Thanh toán VNPAY thành công cho Booking ID: {}", payment.getBookingId());
            try {
                PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                        .paymentId(payment.getId())
                        .bookingId(payment.getBookingId())
                        .userId(payment.getUserId())
                        .amount(payment.getAmount())
                        .transactionId(vnp_TxnRef)
                        .paidAt(payment.getPaidAt())
                        .build();
                OutboxEvent outboxEvent = OutboxEvent.builder()
                        .aggregateId(payment.getId().toString())
                        .aggregateType("PAYMENT")
                        .eventType(PaymentCompletedEvent.class.getSimpleName())
                        .payload(objectMapper.writeValueAsString(event))
                        .isPublished(false)
                        .retryCount(0)
                        .build();
                outboxEventRepository.save(outboxEvent);
                kafkaTemplate.send(PAYMENT_TOPIC, event);
                log.info("🚀 Đã phát sự kiện PaymentCompletedEvent lên Kafka topic 'payment.completed'!");
            }catch (Exception e){
                log.error("Lỗi khi ghi OutboxEvent cho Payment: ", e);
            }
        }else {
            log.warn("Khách hàng hủy thanh toán hoặc thanh toán thất bại (code: {})", vnp_ResponseCode);
            payment.setStatus(PaymentStatus.FAILED);
            try {
                PaymentFailedEvent failedEvent = PaymentFailedEvent.builder()
                        .paymentId(payment.getId())
                        .bookingId(payment.getBookingId())
                        .userId(payment.getUserId())
                        .transactionId(vnp_TxnRef)
                        .failureReason("VNPAY_ERROR_CODE_" + vnp_ResponseCode)
                        .amount(payment.getAmount())
                        .failedAt(Instant.now())
                        .build();
                kafkaTemplate.send("payment.failed", failedEvent);
                log.info("Đã phát sự kiện PaymentFailedEvent lên Kafka topic 'payment.failed'!");
            } catch (Exception e) {
                log.error("Lỗi khi phát sự kiện PaymentFailedEvent: ", e);
            }
        }

        PaymentLog paymentLog = PaymentLog.builder()
                .payment(payment)
                .description("VNPAY Callback - Code: " + vnp_ResponseCode)
                .rawResponse(request.getQueryString())
                .ipAddress(VnpayConfig.getIpAddress(request))
                .build();
        payment.getPaymentLogs().add(paymentLog);
        paymentRepository.save(payment);
        return isSuccess;
    }

    @Override
    public PaymentStatusResponse getPaymentStatusByBookingId(UUID bookingId) {
        Payment payment = paymentRepository.findFirstByBookingIdOrderByCreatedAtDesc(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_EXISTED, "Không tìm thấy thông tin thanh toán cho đơn vé: " + bookingId));

        return PaymentStatusResponse.builder()
                .paymentId(payment.getId())
                .bookingId(payment.getBookingId())
                .provider(payment.getProvider())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .paidAt(payment.getPaidAt())
                .build();
    }
}
