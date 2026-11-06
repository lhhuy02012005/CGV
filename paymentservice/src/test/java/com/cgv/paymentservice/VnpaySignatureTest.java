package com.cgv.paymentservice;

import com.cgv.paymentservice.config.VnpayConfig;
import com.cgv.paymentservice.service.impl.VnpayPayment;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VnpaySignatureTest {
    @Test void actualHmacAcceptsOriginalFieldsAndRejectsTamperedOrMissingSignature() {
        VnpayConfig config = new VnpayConfig();
        config.setSecretKey("test-only-secret");
        VnpayPayment payment = new VnpayPayment(config, null, null, null, null, null);
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("vnp_TxnRef", "fixture-1");
        fields.put("vnp_Amount", "10000");
        fields.put("vnp_CurrCode", "VND");
        fields.put("vnp_SecureHash", config.hashAllFields(fields));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterNames()).thenAnswer(invocation -> Collections.enumeration(fields.keySet()));
        when(request.getParameter(anyString())).thenAnswer(invocation -> fields.get(invocation.getArgument(0)));
        assertTrue(payment.validateVnpayCallback(request));
        fields.put("vnp_Amount", "20000");
        assertFalse(payment.validateVnpayCallback(request));
        fields.remove("vnp_SecureHash");
        assertFalse(payment.validateVnpayCallback(request));
    }
}
