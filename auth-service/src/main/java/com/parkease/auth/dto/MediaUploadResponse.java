package com.parkease.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaUploadResponse {

    private UUID uploadId;

    private String fileUrl;

    private String s3Key;

    private String fileName;

    private Long fileSize;

    private String mimeType;

    private String fileType;

    private LocalDateTime uploadedAt;

    private String uploadStatus;

    private String message;
}
