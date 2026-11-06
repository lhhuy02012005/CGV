package com.cgv.mediaservice.controller;

import com.cgv.commondto.dto.ApiResponse;
import com.cgv.mediaservice.service.MediaService;
import com.cgv.mediaservice.service.CloudinaryUploadService;
import org.springframework.beans.factory.ObjectProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE , makeFinal = true)
public class MediaController {
    ObjectProvider<MediaService> mediaService;
    CloudinaryUploadService cloudinaryUploadService;

    @GetMapping("/cloudinary/upload-signature")
    @PreAuthorize("hasAnyAuthority('movie:manage', 'CONTENT_MANAGER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CloudinaryUploadService.UploadSignature>> getCloudinaryUploadSignature() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(
                ApiResponse.<CloudinaryUploadService.UploadSignature>builder()
                        .data(cloudinaryUploadService.createUploadSignature())
                        .build());
    }

    @GetMapping("/presigned-url")
    public ApiResponse<String> getPresignedUrl(
            @RequestParam("folderName") String folderName,
            @RequestParam("fileName") String fileName,
            @RequestParam("fileType") String fileType
    ) {
        MediaService s3 = mediaService.getIfAvailable();
        if (s3 == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "S3 chưa được cấu hình");
        }
        String presignedUrl = s3.generatePresignedUrl(folderName, fileName, fileType);
        return ApiResponse.<String>builder()
                .message("Tạo Presigned URL thành công")
                .data(presignedUrl)
                .build();
    }
}
