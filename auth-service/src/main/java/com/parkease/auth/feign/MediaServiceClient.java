package com.parkease.auth.feign;

import com.parkease.auth.config.FeignConfig;
import com.parkease.auth.config.MediaServiceFeignConfig;
import com.parkease.auth.dto.ApiResponseWrapper;
import com.parkease.auth.dto.FileUploadResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@FeignClient(
        name = "media-service",
        configuration ={ FeignConfig.class, MediaServiceFeignConfig.class}
)
public interface MediaServiceClient {

    @PostMapping(
            value = "/api/v1/media/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    ApiResponseWrapper<FileUploadResponseDto> uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam("userId") UUID userId,
            @RequestParam("fileType") String fileType,
            @RequestParam(value = "referenceId", required = false) String referenceId,
            @RequestParam(value = "referenceType", required = false) String referenceType
    );

    @GetMapping("/api/v1/media/{uploadId}")
    FileUploadResponseDto getMediaMetadata(@PathVariable("uploadId") UUID uploadId);

    @DeleteMapping("/api/v1/media/{uploadId}")
    void deleteMedia(@PathVariable("uploadId") UUID uploadId);

    @GetMapping("/api/v1/media/health")
    String healthCheck();
}
