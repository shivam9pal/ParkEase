package com.parkease.media.service.impl;

import com.parkease.media.dto.FileMetadataResponse;
import com.parkease.media.dto.FileUploadResponse;
import com.parkease.media.entity.MediaUpload;
import com.parkease.media.exception.FileNotFoundException;
import com.parkease.media.exception.FileUploadException;
import com.parkease.media.repository.MediaUploadRepository;
import com.parkease.media.util.FileValidationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class MediaService {

    @Autowired
    private S3StorageService s3StorageService;

    @Autowired
    private MediaUploadRepository mediaUploadRepository;

    @Autowired
    private FileValidationUtil fileValidationUtil;

    /**
     * Upload file to S3 and save metadata to database
     */
    public FileUploadResponse uploadFile(MultipartFile file, UUID userId, String fileType) {
        return uploadFile(file, userId, fileType, null, null);
    }

    /**
     * Upload file to S3 and save metadata to database with reference
     */
    public FileUploadResponse uploadFile(MultipartFile file, UUID userId, String fileType,
            String referenceId, String referenceType) {
        try {
            log.info("Media service: uploading file. UserId: {}, FileType: {}, FileName: {}",
                    userId, fileType, file.getOriginalFilename());

            // Validate file type
            if (!fileValidationUtil.isValidFileType(fileType)) {
                throw new FileUploadException("Invalid file type: " + fileType, "INVALID_FILE_TYPE");
            }

            // Upload to S3
            String fileUrl = s3StorageService.uploadFile(file, fileType, userId.toString());

            // Generate S3 key
            String s3Key = fileValidationUtil.generateS3Key(fileType, userId.toString(), file.getOriginalFilename());

            // Save metadata to database
            MediaUpload mediaUpload = MediaUpload.builder()
                    .userId(userId)
                    .fileUrl(fileUrl)
                    .fileType(fileType)
                    .fileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .uploadedAt(LocalDateTime.now())
                    .uploadStatus("COMPLETED")
                    .s3Key(s3Key)
                    .referenceId(referenceId)
                    .referenceType(referenceType)
                    .build();

            MediaUpload savedMedia = mediaUploadRepository.save(mediaUpload);

            log.info("File uploaded successfully. UploadId: {}, FileUrl: {}", savedMedia.getId(), fileUrl);

            return FileUploadResponse.success(
                    fileUrl,
                    s3Key,
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType(),
                    fileType,
                    savedMedia.getId()
            );

        } catch (FileUploadException ex) {
            log.error("File upload failed: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error during file upload", ex);
            throw new FileUploadException("File upload failed: " + ex.getMessage(), "UPLOAD_ERROR", ex);
        }
    }

    /**
     * Get file metadata by upload ID
     */
    @Transactional(readOnly = true)
    public FileMetadataResponse getFileMetadata(UUID uploadId) {
        try {
            log.info("Fetching file metadata. UploadId: {}", uploadId);

            MediaUpload mediaUpload = mediaUploadRepository.findByIdAndDeletedAtIsNull(uploadId)
                    .orElseThrow(() -> new FileNotFoundException("File metadata not found", "METADATA_NOT_FOUND"));

            return mapToMetadataResponse(mediaUpload);

        } catch (FileNotFoundException ex) {
            log.warn("File metadata not found: {}", ex.getMessage());
            throw ex;
        }
    }

    /**
     * Get all files uploaded by user
     */
    @Transactional(readOnly = true)
    public List<FileMetadataResponse> getUserFiles(UUID userId) {
        try {
            log.info("Fetching files for user: {}", userId);

            List<MediaUpload> mediaUploads = mediaUploadRepository.findByUserIdAndNotDeleted(userId);

            return mediaUploads.stream()
                    .map(this::mapToMetadataResponse)
                    .collect(Collectors.toList());

        } catch (Exception ex) {
            log.error("Error fetching user files", ex);
            throw new RuntimeException("Failed to fetch user files: " + ex.getMessage());
        }
    }

    /**
     * Get files by type
     */
    @Transactional(readOnly = true)
    public List<FileMetadataResponse> getFilesByType(String fileType) {
        try {
            log.info("Fetching files by type: {}", fileType);

            List<MediaUpload> mediaUploads = mediaUploadRepository.findByFileType(fileType);

            return mediaUploads.stream()
                    .map(this::mapToMetadataResponse)
                    .collect(Collectors.toList());

        } catch (Exception ex) {
            log.error("Error fetching files by type", ex);
            throw new RuntimeException("Failed to fetch files: " + ex.getMessage());
        }
    }

    /**
     * Get files by reference
     */
    @Transactional(readOnly = true)
    public List<FileMetadataResponse> getFilesByReference(String referenceId) {
        try {
            log.info("Fetching files by reference: {}", referenceId);

            List<MediaUpload> mediaUploads = mediaUploadRepository.findByReferenceId(referenceId);

            return mediaUploads.stream()
                    .map(this::mapToMetadataResponse)
                    .collect(Collectors.toList());

        } catch (Exception ex) {
            log.error("Error fetching files by reference", ex);
            throw new RuntimeException("Failed to fetch files: " + ex.getMessage());
        }
    }

    /**
     * Download file from S3
     */
    @Transactional(readOnly = true)
    public byte[] downloadFile(UUID uploadId) {
        try {
            log.info("Downloading file. UploadId: {}", uploadId);

            MediaUpload mediaUpload = mediaUploadRepository.findByIdAndDeletedAtIsNull(uploadId)
                    .orElseThrow(() -> new FileNotFoundException("File not found", "FILE_NOT_FOUND"));

            return s3StorageService.downloadFile(mediaUpload.getS3Key());

        } catch (FileNotFoundException ex) {
            log.warn("File not found for download: {}", ex.getMessage());
            throw ex;
        }
    }

    /**
     * Delete file from S3 and database (soft delete)
     */
    public void deleteFile(UUID uploadId) {
        try {
            log.info("Deleting file. UploadId: {}", uploadId);

            MediaUpload mediaUpload = mediaUploadRepository.findByIdAndDeletedAtIsNull(uploadId)
                    .orElseThrow(() -> new FileNotFoundException("File not found", "FILE_NOT_FOUND"));

            // Delete from S3
            s3StorageService.deleteFile(mediaUpload.getS3Key());

            // Soft delete from database
            mediaUpload.setDeletedAt(LocalDateTime.now());
            mediaUploadRepository.save(mediaUpload);

            log.info("File deleted successfully. UploadId: {}", uploadId);

        } catch (FileNotFoundException ex) {
            log.warn("File not found for deletion: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error deleting file", ex);
            throw new RuntimeException("Failed to delete file: " + ex.getMessage());
        }
    }

    /**
     * Delete file by S3 key (for admin cleanup)
     */
    public void deleteFileByS3Key(String s3Key) {
        try {
            log.info("Deleting file by S3 key: {}", s3Key);

            // Delete from S3
            s3StorageService.deleteFile(s3Key);

            // Soft delete from database
            mediaUploadRepository.findByS3Key(s3Key).ifPresent(media -> {
                media.setDeletedAt(LocalDateTime.now());
                mediaUploadRepository.save(media);
            });

            log.info("File deleted successfully by S3 key: {}", s3Key);

        } catch (Exception ex) {
            log.error("Error deleting file by S3 key", ex);
            throw new RuntimeException("Failed to delete file: " + ex.getMessage());
        }
    }

    /**
     * Get S3 file URL
     */
    @Transactional(readOnly = true)
    public String getFileUrl(UUID uploadId) {
        try {
            log.info("Fetching file URL. UploadId: {}", uploadId);

            MediaUpload mediaUpload = mediaUploadRepository.findByIdAndDeletedAtIsNull(uploadId)
                    .orElseThrow(() -> new FileNotFoundException("File not found", "FILE_NOT_FOUND"));

            return mediaUpload.getFileUrl();

        } catch (FileNotFoundException ex) {
            log.warn("File not found: {}", ex.getMessage());
            throw ex;
        }
    }

    /**
     * Check if file exists
     */
    @Transactional(readOnly = true)
    public boolean fileExists(UUID uploadId) {
        return mediaUploadRepository.findByIdAndDeletedAtIsNull(uploadId).isPresent();
    }

    /**
     * Map MediaUpload entity to FileMetadataResponse DTO
     */
    private FileMetadataResponse mapToMetadataResponse(MediaUpload mediaUpload) {
        return FileMetadataResponse.builder()
                .uploadId(mediaUpload.getId())
                .userId(mediaUpload.getUserId())
                .fileUrl(mediaUpload.getFileUrl())
                .s3Key(mediaUpload.getS3Key())
                .fileName(mediaUpload.getFileName())
                .fileSize(mediaUpload.getFileSize())
                .mimeType(mediaUpload.getMimeType())
                .fileType(mediaUpload.getFileType())
                .referenceId(mediaUpload.getReferenceId())
                .referenceType(mediaUpload.getReferenceType())
                .uploadedAt(mediaUpload.getUploadedAt())
                .uploadStatus(mediaUpload.getUploadStatus())
                .build();
    }

    /**
     * Get user profile picture
     */
    @Transactional(readOnly = true)
    public FileMetadataResponse getUserProfilePicture(UUID userId) {
        try {
            log.info("Fetching profile picture for user: {}", userId);

            List<MediaUpload> profilePics = mediaUploadRepository.findByUserIdAndFileType(userId, "PROFILE_PIC");

            if (profilePics.isEmpty()) {
                throw new FileNotFoundException("Profile picture not found for user: " + userId, "PROFILE_PIC_NOT_FOUND");
            }

            // Return the most recent profile picture
            MediaUpload latestProfilePic = profilePics.get(profilePics.size() - 1);

            return mapToMetadataResponse(latestProfilePic);

        } catch (FileNotFoundException ex) {
            log.warn("Profile picture not found: {}", ex.getMessage());
            throw ex;
        }
    }
}
