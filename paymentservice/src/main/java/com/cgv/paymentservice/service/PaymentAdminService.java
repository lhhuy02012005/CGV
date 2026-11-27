package com.cgv.paymentservice.service;

import com.cgv.commondto.dto.PageResponse;
import com.cgv.commondto.exception.BusinessException;
import com.cgv.commondto.exception.ErrorCode;
import com.cgv.paymentservice.dto.PaymentAdminResponse;
import com.cgv.paymentservice.dto.ReconcileResponse;
import com.cgv.paymentservice.entity.Payment;
import com.cgv.paymentservice.entity.PaymentLog;
import com.cgv.paymentservice.enums.PaymentStatus;
import com.cgv.paymentservice.repository.PaymentRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Quản lý thanh toán cho admin: tra cứu, lọc, đối soát với VNPay và hoàn tiền. */
@Service
@Slf4j(topic = "PAYMENT-ADMIN")
@RequiredArgsConstructor
public class PaymentAdminService {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Pattern UUID_PATTERN =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private final PaymentRepository paymentRepository;
    private final VnpayMerchantClient vnpayMerchantClient;
    private final BookingPaymentAccess bookingPaymentAccess;

    @Transactional(readOnly = true)
    public PageResponse<PaymentAdminResponse> search(String keyword, PaymentStatus status, String provider,
                                                     LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }
        Specification<Payment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                List<Predicate> any = new ArrayList<>(List.of(
                        cb.like(cb.lower(root.get("transactionId")), pattern),
                        cb.like(cb.lower(root.get("userId")), pattern),
                        cb.like(cb.lower(root.get("raw")), pattern)
                ));
                Matcher m = UUID_PATTERN.matcher(keyword);
                if (m.find()) {
                    UUID id = UUID.fromString(m.group());
                    any.add(cb.equal(root.get("bookingId"), id));
                    any.add(cb.equal(root.get("id"), id));
                }
                predicates.add(cb.or(any.toArray(new Predicate[0])));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (provider != null && !provider.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("provider")), provider.trim().toUpperCase()));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay(VN).toInstant()));
            }
            if (toDate != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), toDate.plusDays(1).atStartOfDay(VN).toInstant()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Payment> page = paymentRepository.findAll(spec, pageable);
        return PageResponse.<PaymentAdminResponse>builder()
                .data(page.getContent().stream().map(p -> toResponse(p, false)).toList())
                .pageNumber(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .build();
    }

    @Transactional(readOnly = true)
    public PaymentAdminResponse getDetail(UUID paymentId) {
        return toResponse(findPayment(paymentId), true);
    }

    /**
     * Đối soát: gọi querydr của VNPay rồi so trạng thái + số tiền với bản ghi nội bộ.
     * Chỉ ghi nhận kết quả vào log, không tự sửa trạng thái để tránh xử lý tiền sai.
     */
    @Transactional
    public ReconcileResponse reconcile(UUID paymentId, String ipAddress) {
        Payment payment = findPayment(paymentId);
        requireVnpay(payment);

        VnpayMerchantClient.GatewayResult result = vnpayMerchantClient.queryTransaction(payment, ipAddress);
        if (!result.signatureValid()) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION, "Chữ ký phản hồi VNPay không hợp lệ, không thể đối soát");
        }
        String responseCode = result.get("vnp_ResponseCode");
        String txnStatus = result.get("vnp_TransactionStatus");
        BigDecimal gatewayAmount = parseAmount(result.get("vnp_Amount"));

        boolean matched;
        String message;
        if (!"00".equals(responseCode)) {
            matched = payment.getStatus() != PaymentStatus.SUCCESS && payment.getStatus() != PaymentStatus.REFUNDED;
            message = "VNPay không tìm thấy giao dịch thành công (mã " + responseCode + ": " + result.get("vnp_Message") + ")";
        } else {
            boolean amountOk = gatewayAmount == null || gatewayAmount.compareTo(payment.getAmount()) == 0;
            boolean statusOk = switch (payment.getStatus()) {
                case SUCCESS -> "00".equals(txnStatus);
                case REFUNDED -> Set.of("05", "06").contains(txnStatus) || "00".equals(txnStatus);
                case REFUND_PENDING -> "00".equals(txnStatus) || Set.of("05", "06").contains(txnStatus);
                default -> !"00".equals(txnStatus);
            };
            matched = amountOk && statusOk;
            message = matched
                    ? "Khớp với VNPay"
                    : (!amountOk ? "Lệch số tiền giữa hệ thống và VNPay" : "Lệch trạng thái: hệ thống " + payment.getStatus()
                    + ", VNPay " + describeTxnStatus(txnStatus));
        }

        addLog(payment, "RECONCILE " + (matched ? "MATCHED" : "MISMATCH") + " - " + message, result.responseJson(), ipAddress);
        paymentRepository.save(payment);
        if (!matched) {
            log.warn("Đối soát lệch: payment={} txn={} {}", payment.getId(), payment.getTransactionId(), message);
        }

        return ReconcileResponse.builder()
                .paymentId(payment.getId())
                .transactionId(payment.getTransactionId())
                .localStatus(payment.getStatus())
                .localAmount(payment.getAmount())
                .gatewayResponseCode(responseCode)
                .gatewayTransactionStatus(txnStatus)
                .gatewayAmount(gatewayAmount)
                .gatewayTransactionNo(result.get("vnp_TransactionNo"))
                .gatewayMessage(result.get("vnp_Message"))
                .matched(matched)
                .message(message)
                .build();
    }

    /**
     * Hoàn tiền giao dịch mới nhất của đơn qua VNPay. Trả về null nếu đơn không có giao dịch trực tuyến
     * (VD: đơn 0đ nhờ voucher) để FE tiếp tục hủy vé.
     */
    // Giữ lại log/trạng thái REFUND_PENDING khi báo lỗi cho admin (không rollback).
    @Transactional(noRollbackFor = BusinessException.class)
    public PaymentAdminResponse refundByBooking(UUID bookingId, String actor, String ipAddress) {
        Payment latest = paymentRepository.findFirstByBookingIdOrderByCreatedAtDesc(bookingId).orElse(null);
        if (latest == null) {
            return null;
        }
        Payment payment = paymentRepository.findByIdForUpdate(latest.getId()).orElseThrow();
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return toResponse(payment, false); // đã hoàn trước đó: idempotent
        }
        if (payment.getStatus() != PaymentStatus.SUCCESS && payment.getStatus() != PaymentStatus.REFUND_PENDING) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION,
                    "Chỉ hoàn tiền được giao dịch đã thanh toán thành công (hiện tại: " + payment.getStatus() + ")");
        }
        requireVnpay(payment);

        String bookingStatus = bookingPaymentAccess.bookingStatus(bookingId);
        if (payment.getStatus() == PaymentStatus.SUCCESS && !"CONFIRMED".equals(bookingStatus)) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION,
                    "Chỉ hoàn tiền vé đã xác nhận và chưa soát vé (trạng thái đơn: " + bookingStatus + ")");
        }

        String createdBy = actor == null || actor.isBlank() ? "admin" : actor;
        createdBy = createdBy.length() > 100 ? createdBy.substring(0, 100) : createdBy;
        VnpayMerchantClient.GatewayResult result = vnpayMerchantClient.refund(payment, createdBy, ipAddress);
        String code = result.get("vnp_ResponseCode");

        if (!result.signatureValid()) {
            // Không chắc VNPay đã hoàn hay chưa: khóa giao dịch ở REFUND_PENDING để không gửi hoàn lần 2.
            payment.setStatus(PaymentStatus.REFUND_PENDING);
            addLog(payment, "REFUND UNVERIFIED - chữ ký phản hồi không hợp lệ, cần đối soát thủ công",
                    result.responseJson(), ipAddress);
            paymentRepository.saveAndFlush(payment);
            throw new BusinessException(ErrorCode.INVALID_OPERATION,
                    "Không xác minh được phản hồi hoàn tiền từ VNPay. Giao dịch chuyển sang CHỜ HOÀN TIỀN, vui lòng đối soát.");
        }

        if (!"00".equals(code)) {
            addLog(payment, "REFUND FAILED - code " + code + ": " + result.get("vnp_Message"), result.responseJson(), ipAddress);
            paymentRepository.save(payment);
            throw new BusinessException(ErrorCode.INVALID_OPERATION,
                    "VNPay từ chối hoàn tiền (mã " + code + "): " + result.get("vnp_Message"));
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundAmount(payment.getAmount());
        payment.setRefundedAt(Instant.now());
        addLog(payment, "REFUND SUCCESS by " + createdBy + " - VNPay txn " + result.get("vnp_TransactionNo"),
                result.responseJson(), ipAddress);
        Payment saved = paymentRepository.save(payment);
        log.info("Đã hoàn tiền VNPay cho booking={} payment={}", bookingId, payment.getId());
        return toResponse(saved, false);
    }

    private Payment findPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_EXISTED, "Không tìm thấy giao dịch: " + paymentId));
    }

    private static void requireVnpay(Payment payment) {
        if (!"VNPAY".equalsIgnoreCase(payment.getProvider())) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION, "Chỉ hỗ trợ đối soát/hoàn tiền cho cổng VNPay");
        }
    }

    private static void addLog(Payment payment, String description, String rawResponse, String ip) {
        payment.getPaymentLogs().add(PaymentLog.builder()
                .payment(payment)
                .description(description)
                .rawResponse(rawResponse)
                .ipAddress(ip)
                .build());
    }

    private static BigDecimal parseAmount(String vnpAmount) {
        try {
            return vnpAmount == null || vnpAmount.isBlank() ? null : new BigDecimal(vnpAmount).movePointLeft(2);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String describeTxnStatus(String code) {
        return switch (code == null ? "" : code) {
            case "00" -> "thành công";
            case "01" -> "chưa hoàn tất";
            case "02" -> "lỗi";
            case "04" -> "giao dịch đảo";
            case "05" -> "đang xử lý hoàn tiền";
            case "06" -> "đã gửi yêu cầu hoàn tiền";
            case "07" -> "nghi ngờ gian lận";
            case "09" -> "hoàn tiền bị từ chối";
            default -> "mã " + code;
        };
    }

    private PaymentAdminResponse toResponse(Payment p, boolean withLogs) {
        return PaymentAdminResponse.builder()
                .id(p.getId())
                .bookingId(p.getBookingId())
                .userId(p.getUserId())
                .provider(p.getProvider())
                .paymentMethod(p.getPaymentMethod())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .status(p.getStatus())
                .transactionId(p.getTransactionId())
                .gatewayTransactionNo(VnpayMerchantClient.gatewayTransactionNo(p))
                .createdAt(p.getCreatedAt())
                .paidAt(p.getPaidAt())
                .refundAmount(p.getRefundAmount())
                .refundedAt(p.getRefundedAt())
                .logs(withLogs ? p.getPaymentLogs().stream()
                        .sorted(Comparator.comparing(PaymentLog::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(l -> PaymentAdminResponse.LogEntry.builder()
                                .id(l.getId())
                                .description(l.getDescription())
                                .rawResponse(l.getRawResponse())
                                .ipAddress(l.getIpAddress())
                                .createdAt(l.getCreatedAt())
                                .build())
                        .toList() : null)
                .build();
    }
}
