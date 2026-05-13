package com.parkease.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReceiptResponse {

    private String s3Url;           // If receipt already exists in S3
    private byte[] pdfBytes;        // If receipt was just generated
    private boolean fromS3;         // Boolean flag: true if S3 URL, false if PDF bytes
    private String message;

    public static ReceiptResponse fromS3Url(String s3Url) {
        return ReceiptResponse.builder()
                .s3Url(s3Url)
                .fromS3(true)
                .message("Receipt available in S3. Download from provided URL.")
                .build();
    }

    public static ReceiptResponse fromPdfBytes(byte[] pdfBytes) {
        return ReceiptResponse.builder()
                .pdfBytes(pdfBytes)
                .fromS3(false)
                .message("Receipt generated. S3 upload in progress.")
                .build();
    }
}
