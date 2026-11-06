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
import com.cgv.paymentservice.service.BookingPaymentAccess;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    OutboxEventRepository outboxEventRepository;
    RedisTemplate<String , Object> redisTemplate;
    BookingPaymentAccess bookingPaymentAccess;

    int PAYMENT_DEADLINE_DURATION = 10;
    String PAYMENT_TOPIC = "payment.completed";
    @Override
    public String add(HttpServletRequest request , UUID bookingId, BigDecimal amount, String userId, Instant deadline) {
       try {
           if (deadline == null || !deadline.isAfter(Instant.now())) {
               throw new BusinessException(ErrorCode.BAD_REQUEST, "Đơn đã hết hạn thanh toán");
           }
           var existing = paymentRepository.findFirstByBookingIdOrderByCreatedAtDesc(bookingId);
           if (existing.isPresent()) {
               if (existing.get().getStatus() == PaymentStatus.PENDING
                       && existing.get().getAmount().compareTo(amount) == 0
                       && existing.get().getRaw() != null && !existing.get().getRaw().isBlank())
                   return existing.get().getRaw();
               throw new BusinessException(ErrorCode.INVALID_OPERATION, "Đơn đã có giao dịch VNPay; cần đối soát trước khi thanh toán lại");
           }
           // VNPay yêu cầu nhân 100
           long vnpAmount = amount.movePointRight(2).longValueExact();
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

           TimeZone tz = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
           Calendar cld = Calendar.getInstance(tz);
           SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
           formatter.setTimeZone(tz);
           String createDate = formatter.format(cld.getTime());
           vnp_Params.put("vnp_CreateDate", createDate);
           vnp_Params.put("vnp_ExpireDate", DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                   .withZone(ZoneId.of("Asia/Ho_Chi_Minh")).format(deadline));

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
           log.info("VNPay URL generated for booking {}", bookingId);

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
       }catch (BusinessException e){
           throw e;
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
        return vnp_SecureHash != null && java.security.MessageDigest.isEqual(
                signValue.getBytes(StandardCharsets.UTF_8), vnp_SecureHash.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public boolean callback(HttpServletRequest request){
        String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
        String vnp_TxnRef = request.getParameter("vnp_TxnRef");
        String vnp_BankCode = request.getParameter("vnp_BankCode");
        String vnp_TransactionNo = request.getParameter("vnp_TransactionNo");

        log.info("VNPay return: vnp_ResponseCode={}, vnp_TxnRef={}", vnp_ResponseCode, vnp_TxnRef);


        boolean validSignature = validateVnpayCallback(request);

        if (!validSignature) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Chữ ký VNPAY không hợp lệ!");
        }

        if (vnp_TxnRef == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "Thiếu mã giao dịch");
        Payment payment = paymentRepository.findByTransactionIdForUpdate(vnp_TxnRef)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "Không tìm thấy giao dịch: " + vnp_TxnRef));
        if (!vnpayConfig.getVnpTmnCode().equals(request.getParameter("vnp_TmnCode"))
                || !"VND".equals(request.getParameter("vnp_CurrCode"))
                || !"00".equals(request.getParameter("vnp_TransactionStatus")) && "00".equals(vnp_ResponseCode)
                || !String.valueOf(payment.getAmount().movePointRight(2).longValueExact())
                        .equals(request.getParameter("vnp_Amount"))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Thông tin VNPay không khớp đơn thanh toán");
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.warn("Giao dịch {} đã được xử lý trước đó.", vnp_TxnRef);
            return true;
        }

        if (payment.getStatus() != PaymentStatus.PENDING) { return false; }
        boolean isSuccess = "00".equals(vnp_ResponseCode);
        if (isSuccess && !bookingPaymentAccess.lockAndCheckBooking(payment.getBookingId(), payment.getAmount())) {
            payment.setStatus(PaymentStatus.REFUND_PENDING);
            payment.setPaidAt(Instant.now());
            payment.setRaw("VNPAY_TRANS_NO:" + vnp_TransactionNo);
            paymentRepository.save(payment);
            log.error("PAYMENT_REQUIRES_REFUND booking={} transaction={}; reconcile with VNPay", payment.getBookingId(), vnp_TxnRef);
            return false;
        }
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

                log.info("Đã lưu Outbox PaymentCompletedEvent cho topic 'payment.completed'!");
            }catch (Exception e){
                throw new IllegalStateException("Cannot persist payment outbox", e);
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

                OutboxEvent outboxEvent = OutboxEvent.builder()
                        .aggregateId(payment.getId().toString())
                        .aggregateType("PAYMENT")
                        .eventType(PaymentFailedEvent.class.getSimpleName())
                        .payload(objectMapper.writeValueAsString(failedEvent))
                        .isPublished(false)
                        .retryCount(0)
                        .build();
                outboxEventRepository.save(outboxEvent);

                log.info("Đã lưu Outbox PaymentFailedEvent cho topic 'payment.failed'!");
            } catch (Exception e) {
                throw new IllegalStateException("Cannot persist failed-payment outbox", e);
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
