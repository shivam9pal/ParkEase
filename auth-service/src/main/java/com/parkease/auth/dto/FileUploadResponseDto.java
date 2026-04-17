package com.parkease.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * File upload response from Media Service Matches: FileUploadResponse
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadResponseDto {

    private UUID uploadId;

    private String fileUrl;

    private String s3Key;

    private String fileName;

    private Long fileSize;

    private String mimeType;

    private String fileType;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadedAt;

    private String uploadStatus;

    private String message;
}
