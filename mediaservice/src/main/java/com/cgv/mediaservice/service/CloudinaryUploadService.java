package com.cgv.mediaservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class CloudinaryUploadService {
    private static final String FOLDER = "cgv/movies";

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    public CloudinaryUploadService(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    public UploadSignature createUploadSignature() {
        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Cloudinary chưa được cấu hình");
        }
        long timestamp = Instant.now().getEpochSecond();
        return new UploadSignature(cloudName, apiKey, timestamp, FOLDER, sign(timestamp));
    }

    String sign(long timestamp) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(
                    ("folder=" + FOLDER + "&timestamp=" + timestamp + apiSecret).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-1 unavailable", ex);
        }
    }

    public record UploadSignature(String cloudName, String apiKey, long timestamp, String folder, String signature) {
    }
}
