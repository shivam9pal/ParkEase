package com.parkease.media.service;

import com.parkease.media.dto.MediaResponse;
import com.parkease.media.dto.UploadRequest;
import com.parkease.media.dto.UploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface MediaService {

    /**
     * Upload a file and store metadata
     */
    UploadResponse uploadFile(MultipartFile file, UploadRequest request);

    /**
     * Get media by ID
     */
    MediaResponse getMediaById(UUID mediaId);

    /**
     * Get all media in a folder
     */
    List<MediaResponse> getMediaByFolder(String folder);

    /**
     * Get all media for a reference (e.g., userId, lotId)
     */
    List<MediaResponse> getMediaByReference(String reference);

    /**
     * Soft delete (mark as deleted)
     */
    void deleteMedia(UUID mediaId);

    /**
     * Hard delete from S3 and database
     */
    void hardDeleteMedia(UUID mediaId);

    /**
     * Generate presigned URL for download
     */
    String generatePresignedUrl(UUID mediaId);
}
