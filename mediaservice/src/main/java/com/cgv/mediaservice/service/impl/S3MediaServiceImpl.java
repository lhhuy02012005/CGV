package com.cgv.mediaservice.service.impl;

import com.cgv.mediaservice.service.MediaService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;

@Service
public class S3MediaServiceImpl implements MediaService {
    private final S3Presigner s3Presigner;
    private final String bucketName;
    public S3MediaServiceImpl(
            @Value("${aws.s3.region}") String region,
            @Value("${aws.s3.bucket-name}") String bucketName,
            @Value("${aws.s3.access-key}") String accessKey,
            @Value("${aws.s3.secret-key}") String secretKey) {

        this.bucketName = bucketName;

        this.s3Presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
    }


    @Override
    public String generatePresignedUrl(String folderName, String fileName, String fileType) {
        String initFileName = System.currentTimeMillis() + "-" + fileName.replace("\\s+", "_");
        String objKey = folderName + "/" + initFileName;

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objKey)
                .contentType(fileType)
                .build();
        
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(objectRequest)
                .build();

        URL url = s3Presigner.presignPutObject(presignRequest).url();
        return url.toString();
    }
}
