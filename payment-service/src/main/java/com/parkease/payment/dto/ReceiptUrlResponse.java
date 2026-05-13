package com.parkease.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptUrlResponse {

    private String s3Url;
    private String message;
    private long timestamp;

    public static ReceiptUrlResponse fromS3Url(String s3Url) {
        return ReceiptUrlResponse.builder()
                .s3Url(s3Url)
                .message("Receipt available in S3. Download from provided URL.")
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
