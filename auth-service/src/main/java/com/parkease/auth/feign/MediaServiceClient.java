package com.parkease.auth.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * Feign client for media-service Handles file uploads to AWS S3 via
 * media-service
 */
@FeignClient(name = "media-service")
public interface MediaServiceClient {

    /**
     * Upload a file to S3
     *
     * @param file The file to upload (multipart)
     * @param folder The S3 folder: "auth", "parking", "vehicle", "receipts"
     * @param reference The entity reference (userId, lotId, etc.)
     * @param uploadedBy The user who uploaded (userId)
     * @param description Optional description
     * @return MediaUploadResponse with s3Url, mediaId, etc.
     */
    @PostMapping(value = "/api/media/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    MediaUploadResponse uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam("folder") String folder,
            @RequestParam("reference") String reference,
            @RequestParam(value = "uploadedBy", required = false) String uploadedBy,
            @RequestParam(value = "description", required = false) String description
    );
}
