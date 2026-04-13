package com.parkease.media.dto;

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
public class UploadResponse {

    private UUID mediaId;

    private String fileName;

    private String s3Url;

    private String presignedUrl;

    private Long fileSize;

    private String mimeType;

    private LocalDateTime uploadedAt;
}
