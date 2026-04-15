package com.parkease.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadRequest {

    private UUID userId;

    private String fileType;  // PROFILE_PIC, LOT_IMAGE, RECEIPT, VEHICLE_DOC

    private String referenceId;  // UUID of related entity

    private String referenceType;  // PAYMENT, PARKING_LOT, USER, VEHICLE

    // File data will be sent as multipart/form-data
    // The file itself is handled by MultipartFile in the controller
}
