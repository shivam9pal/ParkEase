package com.parkease.media.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "media_upload")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 1000)
    private String fileUrl;

    @Column(length = 50)
    private String fileType;  // PROFILE_PIC, LOT_IMAGE, RECEIPT, VEHICLE_DOC, etc

    @Column(length = 255)
    private String fileName;  // Original filename

    @Column
    private Long fileSize;  // Size in bytes

    @Column(length = 100)
    private String mimeType;  // image/jpeg, application/pdf, etc

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Column
    private LocalDateTime deletedAt;  // For soft delete

    @Column(length = 50)
    private String uploadStatus;  // COMPLETED, FAILED, IN_PROGRESS

    @Column(length = 500)
    private String s3Key;  // S3 object key for direct deletion/retrieval

    @Column(length = 1000)
    private String referenceId;  // UUID of related entity (Payment, ParkingLot, User, etc)

    @Column(length = 100)
    private String referenceType;  // PAYMENT, PARKING_LOT, USER, VEHICLE, etc

    @Column(length = 500)
    private String uploadErrorMessage;  // Error details if upload failed

    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
        if (uploadStatus == null) {
            uploadStatus = "COMPLETED";
        }
    }
}
