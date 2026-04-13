package com.parkease.auth.feign;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response from media-service upload endpoint
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadResponse {

    private UUID mediaId;

    private String fileName;

    private String s3Url;

    private String presignedUrl;

    private Long fileSize;

    private String mimeType;

    private LocalDateTime uploadedAt;
}
