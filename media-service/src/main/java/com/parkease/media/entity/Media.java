package com.parkease.media.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "media")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String s3Key;  // Full path in S3 bucket

    @Column(nullable = false)
    private String s3Url;  // S3 URL

    @Column(nullable = false)
    private String mimeType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private String folder;  // e.g., "auth", "parking", "vehicle", "receipts"

    @Column
    private String uploadedBy;  // userId or system

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Column
    private String description;

    @Column(nullable = false)
    private Boolean isDeleted;

    @Column
    private LocalDateTime deletedAt;

    @Column
    private String reference;  // Reference to entity (e.g., userId, lotId, paymentId)
}
