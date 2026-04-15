package com.parkease.media.controller;

import com.parkease.media.dto.ApiResponse;
import com.parkease.media.dto.FileMetadataResponse;
import com.parkease.media.dto.FileUploadResponse;
import com.parkease.media.service.impl.MediaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class MediaController {

    @Autowired
    private MediaService mediaService;

    /**
     * Upload file to S3 POST /api/v1/media/upload Parameters: - file:
     * MultipartFile (form data) - userId: UUID - fileType: String (PROFILE_PIC,
     * LOT_IMAGE, RECEIPT, VEHICLE_DOC) - referenceId: String (optional) -
     * referenceType: String (optional)
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") UUID userId,
            @RequestParam("fileType") String fileType,
            @RequestParam(value = "referenceId", required = false) String referenceId,
            @RequestParam(value = "referenceType", required = false) String referenceType) {

        log.info("Upload file request received. UserId: {}, FileType: {}, FileName: {}",
                userId, fileType, file.getOriginalFilename());

        try {
            FileUploadResponse response = mediaService.uploadFile(file, userId, fileType, referenceId, referenceType);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "File uploaded successfully"));
        } catch (Exception ex) {
            log.error("Error uploading file", ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(ex.getMessage(), "UPLOAD_FAILED"));
        }
    }

    /**
     * Get file metadata by upload ID GET /api/v1/media/{uploadId}/metadata
     */
    @GetMapping("/{uploadId}/metadata")
    public ResponseEntity<ApiResponse<FileMetadataResponse>> getFileMetadata(
            @PathVariable UUID uploadId) {

        log.info("Get file metadata request. UploadId: {}", uploadId);

        try {
            FileMetadataResponse response = mediaService.getFileMetadata(uploadId);
            return ResponseEntity.ok(ApiResponse.success(response, "File metadata retrieved successfully"));
        } catch (Exception ex) {
            log.error("Error fetching file metadata", ex);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(ex.getMessage(), "METADATA_NOT_FOUND"));
        }
    }

    /**
     * Download file from S3 GET /api/v1/media/{uploadId}/download
     */
    @GetMapping("/{uploadId}/download")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable UUID uploadId) {

        log.info("Download file request. UploadId: {}", uploadId);

        try {
            FileMetadataResponse metadata = mediaService.getFileMetadata(uploadId);
            byte[] fileContent = mediaService.downloadFile(uploadId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getFileName() + "\"")
                    .contentType(MediaType.valueOf(metadata.getMimeType() != null ? metadata.getMimeType() : "application/octet-stream"))
                    .body(fileContent);

        } catch (Exception ex) {
            log.error("Error downloading file", ex);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Get file URL GET /api/v1/media/{uploadId}/url
     */
    @GetMapping("/{uploadId}/url")
    public ResponseEntity<ApiResponse<String>> getFileUrl(
            @PathVariable UUID uploadId) {

        log.info("Get file URL request. UploadId: {}", uploadId);

        try {
            String fileUrl = mediaService.getFileUrl(uploadId);
            return ResponseEntity.ok(ApiResponse.success(fileUrl, "File URL retrieved successfully"));
        } catch (Exception ex) {
            log.error("Error fetching file URL", ex);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(ex.getMessage(), "FILE_NOT_FOUND"));
        }
    }

    /**
     * Get all files for a user GET /api/v1/media/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<FileMetadataResponse>>> getUserFiles(
            @PathVariable UUID userId) {

        log.info("Get user files request. UserId: {}", userId);

        try {
            List<FileMetadataResponse> files = mediaService.getUserFiles(userId);
            return ResponseEntity.ok(ApiResponse.success(files, "User files retrieved successfully"));
        } catch (Exception ex) {
            log.error("Error fetching user files", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ex.getMessage(), "FETCH_ERROR"));
        }
    }

    /**
     * Get user profile picture GET /api/v1/media/user/{userId}/profile-picture
     */
    @GetMapping("/user/{userId}/profile-picture")
    public ResponseEntity<ApiResponse<FileMetadataResponse>> getUserProfilePicture(
            @PathVariable UUID userId) {

        log.info("Get user profile picture request. UserId: {}", userId);

        try {
            FileMetadataResponse profilePic = mediaService.getUserProfilePicture(userId);
            return ResponseEntity.ok(ApiResponse.success(profilePic, "User profile picture retrieved successfully"));
        } catch (Exception ex) {
            log.error("Error fetching user profile picture", ex);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(ex.getMessage(), "PROFILE_PIC_NOT_FOUND"));
        }
    }

    /**
     * Get files by type GET /api/v1/media/type/{fileType}
     */
    @GetMapping("/type/{fileType}")
    public ResponseEntity<ApiResponse<List<FileMetadataResponse>>> getFilesByType(
            @PathVariable String fileType) {

        log.info("Get files by type request. FileType: {}", fileType);

        try {
            List<FileMetadataResponse> files = mediaService.getFilesByType(fileType);
            return ResponseEntity.ok(ApiResponse.success(files, "Files retrieved successfully"));
        } catch (Exception ex) {
            log.error("Error fetching files by type", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ex.getMessage(), "FETCH_ERROR"));
        }
    }

    /**
     * Get files by reference GET /api/v1/media/reference/{referenceId}
     */
    @GetMapping("/reference/{referenceId}")
    public ResponseEntity<ApiResponse<List<FileMetadataResponse>>> getFilesByReference(
            @PathVariable String referenceId) {

        log.info("Get files by reference request. ReferenceId: {}", referenceId);

        try {
            List<FileMetadataResponse> files = mediaService.getFilesByReference(referenceId);
            return ResponseEntity.ok(ApiResponse.success(files, "Files retrieved successfully"));
        } catch (Exception ex) {
            log.error("Error fetching files by reference", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ex.getMessage(), "FETCH_ERROR"));
        }
    }

    /**
     * Delete file DELETE /api/v1/media/{uploadId}
     */
    @DeleteMapping("/{uploadId}")
    public ResponseEntity<ApiResponse<String>> deleteFile(
            @PathVariable UUID uploadId) {

        log.info("Delete file request. UploadId: {}", uploadId);

        try {
            mediaService.deleteFile(uploadId);
            return ResponseEntity.ok(ApiResponse.success(null, "File deleted successfully"));
        } catch (Exception ex) {
            log.error("Error deleting file", ex);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(ex.getMessage(), "DELETE_FAILED"));
        }
    }

    /**
     * Health check endpoint GET /api/v1/media/health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        log.info("Health check endpoint called");
        return ResponseEntity.ok(ApiResponse.success("OK", "Media service is running"));
    }
}
