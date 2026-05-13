package com.parkease.payment.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.parkease.payment.entity.Payment;
import com.parkease.payment.feign.MediaServiceClient;
import com.parkease.payment.repository.PaymentRepository;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

@Service
@Slf4j
public class S3ReceiptUploadService {

    @Autowired
    private MediaServiceClient mediaServiceClient;

    @Autowired
    private PaymentRepository paymentRepository;

    /**
     * Synchronous method to upload receipt PDF to S3 via Media Service. BLOCKS
     * until S3 upload completes, ensuring Spring Cloud context is preserved.
     * Returns S3 URL on success, throws exception on failure.
     */
    public String uploadReceiptToS3Sync(UUID paymentId, UUID userId, byte[] pdfBytes) {
        try {
            log.info("[S3Upload] Starting SYNCHRONOUS upload for paymentId={}, size={} bytes", paymentId, pdfBytes.length);

            // Get payment entity
            Payment payment = paymentRepository.findByPaymentId(paymentId)
                    .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

            // Create MultipartFile from byte array
            MultipartFile multipartFile = new ByteArrayMultipartFile(
                    pdfBytes,
                    "receipt_" + paymentId + ".pdf",
                    "application/pdf"
            );

            // Call Media Service to upload to S3 (SYNCHRONOUSLY - blocks until complete)
            log.info("[S3Upload] Calling Media Service for paymentId={} (SYNC - will wait)", paymentId);
            var response = mediaServiceClient.uploadFile(
                    multipartFile,
                    userId,
                    "RECEIPT",
                    paymentId.toString(),
                    "PAYMENT"
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                MediaServiceClient.MediaUploadResponse uploadResponse = response.getBody();
                if (uploadResponse.data != null && uploadResponse.data.fileUrl != null) {
                    // Save S3 URL to payment entity for future cache hits
                    payment.setReceiptPath(uploadResponse.data.fileUrl);
                    payment.setReceiptUploadId(uploadResponse.data.uploadId);
                    paymentRepository.save(payment);

                    log.info("[S3Upload] Receipt uploaded successfully to S3 (SYNC complete). PaymentId={}, S3Url={}",
                            paymentId, uploadResponse.data.fileUrl);
                    return uploadResponse.data.fileUrl;
                } else {
                    throw new RuntimeException("Media Service returned empty file URL for paymentId=" + paymentId);
                }
            } else {
                throw new RuntimeException("Media Service upload failed for paymentId=" + paymentId + ". Status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("[S3Upload] SYNC S3 upload FAILED for paymentId={}: {}",
                    paymentId, e.getMessage(), e);
            throw new RuntimeException("Failed to upload receipt to S3: " + e.getMessage(), e);
        }
    }

    /**
     * Async method to upload receipt PDF to S3 via Media Service (deprecated -
     * use uploadReceiptToS3Sync) Runs in background thread pool
     */
    @Async
    @Deprecated
    public void uploadReceiptToS3(UUID paymentId, UUID userId, byte[] pdfBytes) {
        try {
            log.info("[S3Upload] Starting async upload for paymentId={}, size={} bytes", paymentId, pdfBytes.length);

            // Get payment entity
            Payment payment = paymentRepository.findByPaymentId(paymentId)
                    .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

            // Create MultipartFile from byte array
            MultipartFile multipartFile = new ByteArrayMultipartFile(
                    pdfBytes,
                    "receipt_" + paymentId + ".pdf",
                    "application/pdf"
            );

            // Call Media Service to upload to S3
            log.info("[S3Upload] Uploading to Media Service for paymentId={}", paymentId);
            var response = mediaServiceClient.uploadFile(
                    multipartFile,
                    userId,
                    "RECEIPT",
                    paymentId.toString(),
                    "PAYMENT"
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                MediaServiceClient.MediaUploadResponse uploadResponse = response.getBody();
                if (uploadResponse.data != null && uploadResponse.data.fileUrl != null) {
                    // Save S3 URL to payment entity
                    payment.setReceiptPath(uploadResponse.data.fileUrl);
                    payment.setReceiptUploadId(uploadResponse.data.uploadId);
                    paymentRepository.save(payment);

                    log.info("[S3Upload] Receipt uploaded successfully to S3. PaymentId={}, S3Url={}",
                            paymentId, uploadResponse.data.fileUrl);
                } else {
                    log.warn("[S3Upload] Media Service returned empty response for paymentId={}", paymentId);
                }
            } else {
                log.error("[S3Upload] Media Service upload failed for paymentId={}. Status: {}",
                        paymentId, response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("[S3Upload] Failed to upload receipt to S3 for paymentId={}: {}",
                    paymentId, e.getMessage(), e);
            // Don't throw exception - async task should not propagate failures
        }
    }

    /**
     * Wrapper class to convert byte array to MultipartFile
     */
    private static class ByteArrayMultipartFile implements MultipartFile {

        private final byte[] fileBytes;
        private final String filename;
        private final String contentType;

        public ByteArrayMultipartFile(byte[] fileBytes, String filename, String contentType) {
            this.fileBytes = fileBytes;
            this.filename = filename;
            this.contentType = contentType;
        }

        @Override
        public String getName() {
            return filename;
        }

        @Override
        public String getOriginalFilename() {
            return filename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return fileBytes.length == 0;
        }

        @Override
        public long getSize() {
            return fileBytes.length;
        }

        @Override
        public byte[] getBytes() {
            return fileBytes;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(fileBytes);
        }

        @Override
        public void transferTo(java.io.File dest) throws java.io.IOException {
            java.nio.file.Files.write(dest.toPath(), fileBytes);
        }

        @Override
        public void transferTo(java.nio.file.Path dest) throws java.io.IOException {
            java.nio.file.Files.write(dest, fileBytes);
        }
    }
}
