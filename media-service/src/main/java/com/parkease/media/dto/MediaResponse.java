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
public class MediaResponse {

    private UUID id;

    private String fileName;

    private String s3Url;

    private String mimeType;

    private Long fileSize;

    private String folder;

    private String uploadedBy;

    private LocalDateTime uploadedAt;

    private String description;

    private String reference;
}
