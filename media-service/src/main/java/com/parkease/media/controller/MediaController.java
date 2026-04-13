package com.parkease.media.controller;

import com.parkease.media.dto.MediaResponse;
import com.parkease.media.dto.UploadRequest;
import com.parkease.media.dto.UploadResponse;
import com.parkease.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("folder") String folder,
            @RequestParam("reference") String reference,
            @RequestParam(value = "uploadedBy", required = false) String uploadedBy,
            @RequestParam(value = "description", required = false) String description
    ) {
        UploadRequest request = new UploadRequest(folder, uploadedBy, description, reference);
        UploadResponse response = mediaService.uploadFile(file, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get media by ID
     */
    @GetMapping("/{mediaId}")
    public ResponseEntity<MediaResponse> getMedia(@PathVariable UUID mediaId) {
        MediaResponse response = mediaService.getMediaById(mediaId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all media by folder
     */
    @GetMapping("/folder/{folder}")
    public ResponseEntity<List<MediaResponse>> getMediaByFolder(@PathVariable String folder) {
        List<MediaResponse> response = mediaService.getMediaByFolder(folder);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all media by reference (userId, lotId, etc.)
     */
    @GetMapping("/reference/{reference}")
    public ResponseEntity<List<MediaResponse>> getMediaByReference(@PathVariable String reference) {
        List<MediaResponse> response = mediaService.getMediaByReference(reference);
        return ResponseEntity.ok(response);
    }

    /**
     * Get presigned URL for download
     */
    @GetMapping("/{mediaId}/presigned-url")
    public ResponseEntity<String> getPresignedUrl(@PathVariable UUID mediaId) {
        String url = mediaService.generatePresignedUrl(mediaId);
        return ResponseEntity.ok(url);
    }

    /**
     * Soft delete media (mark as deleted)
     */
    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Void> deleteMedia(@PathVariable UUID mediaId) {
        mediaService.deleteMedia(mediaId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Hard delete media (remove from S3 and database)
     */
    @DeleteMapping("/{mediaId}/hard")
    public ResponseEntity<Void> hardDeleteMedia(@PathVariable UUID mediaId) {
        mediaService.hardDeleteMedia(mediaId);
        return ResponseEntity.noContent().build();
    }
}
