package com.parkease.media.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadRequest {

    private String folder;  // "auth", "parking", "vehicle", "receipts"

    private String uploadedBy;  // userId or system

    private String description;

    private String reference;  // Identifier (userId, lotId, paymentId)
}
