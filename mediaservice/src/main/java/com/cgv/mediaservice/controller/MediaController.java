package com.cgv.mediaservice.controller;

import com.cgv.commondto.dto.ApiResponse;
import com.cgv.mediaservice.service.MediaService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE , makeFinal = true)
public class MediaController {
    MediaService mediaService;

    @GetMapping("/presigned-url")
    public ApiResponse<String> getPresignedUrl(
            @RequestParam("folderName") String folderName,
            @RequestParam("fileName") String fileName,
            @RequestParam("fileType") String fileType
    ) {
        String presignedUrl = mediaService.generatePresignedUrl(folderName, fileName, fileType);
        return ApiResponse.<String>builder()
                .message("Tạo Presigned URL thành công")
                .data(presignedUrl)
                .build();
    }
}
