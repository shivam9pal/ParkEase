package com.parkease.payment.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@FeignClient(
        name = "media-service",
        configuration = FeignConfig.class
)
public interface MediaServiceClient {

    @PostMapping(
            value = "/api/v1/media/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<MediaUploadResponse> uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam("userId") UUID userId,
            @RequestParam("fileType") String fileType,
            @RequestParam(value = "referenceId", required = false) String referenceId,
            @RequestParam(value = "referenceType", required = false) String referenceType
    );

    // ─── Response DTO ──────────────────────────────────────────────────────────
    class MediaUploadResponse {

        public boolean success;
        public MediaUploadData data;

        public static class MediaUploadData {

            public String fileUrl;
            public UUID uploadId;
            public String s3Key;
            public String fileName;
        }
    }
}
