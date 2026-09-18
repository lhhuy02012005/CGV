package com.cgv.mediaservice.service;

public interface MediaService {
    /**
     * Tạo Presigned URL để upload file trực tiếp lên S3
     *
     * @param folderName Tên thư mục phân loại (VD: "posters", "avatars", "banners")
     * @param fileName   Tên gốc của file (VD: "inception-poster.jpg")
     * @param fileType   Mime type của file (VD: "image/jpeg", "image/png")
     * @return Đường dẫn Presigned URL có thời hạn ngắn để Frontend upload
     */
    String generatePresignedUrl(String folderName, String fileName, String fileType);
}
