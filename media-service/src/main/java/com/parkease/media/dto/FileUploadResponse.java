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
public class FileUploadResponse {

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

    public static FileUploadResponse success(String fileUrl, String s3Key, String fileName,
            Long fileSize, String mimeType, String fileType,
            UUID uploadId) {
        return FileUploadResponse.builder()
                .uploadId(uploadId)
                .fileUrl(fileUrl)
                .s3Key(s3Key)
                .fileName(fileName)
                .fileSize(fileSize)
                .mimeType(mimeType)
                .fileType(fileType)
                .uploadedAt(LocalDateTime.now())
                .uploadStatus("COMPLETED")
                .message("File uploaded successfully")
                .build();
    }

    public static FileUploadResponse error(String errorMessage) {
        return FileUploadResponse.builder()
                .uploadStatus("FAILED")
                .message(errorMessage)
                .uploadedAt(LocalDateTime.now())
                .build();
    }
}
