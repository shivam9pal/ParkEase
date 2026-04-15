package com.parkease.media.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class FileMetadataResponse {

    private UUID uploadId;

    private UUID userId;

    private String fileUrl;

    private String s3Key;

    private String fileName;

    private Long fileSize;

    private String mimeType;

    private String fileType;

    private String referenceId;

    private String referenceType;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadedAt;

    private String uploadStatus;
}
