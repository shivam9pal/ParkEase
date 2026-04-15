package com.parkease.media.service.impl;

import com.parkease.media.exception.FileUploadException;
import com.parkease.media.util.FileValidationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class S3StorageService {

    @Autowired
    private S3Client s3Client;

    @Autowired
    private FileValidationUtil fileValidationUtil;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    /**
     * Upload file to AWS S3
     */
    public String uploadFile(MultipartFile file, String fileType, String userId) {
        try {
            log.info("Starting file upload to S3. File: {}, Type: {}, UserId: {}",
                    file.getOriginalFilename(), fileType, userId);

            // Validate file
            fileValidationUtil.validateFile(file);

            // Generate S3 key
            String s3Key = fileValidationUtil.generateS3Key(fileType, userId, file.getOriginalFilename());

            // Get file content
            byte[] fileContent = file.getBytes();

            // Build metadata
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("original-filename", file.getOriginalFilename());
            metadata.put("user-id", userId);
            metadata.put("file-type", fileType);
            metadata.put("upload-timestamp", String.valueOf(System.currentTimeMillis()));

            // Upload to S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength((long) fileContent.length)
                    .metadata(metadata)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileContent));

            // Generate and return S3 URL
            String fileUrl = generateS3Url(s3Key);

            log.info("File uploaded successfully to S3. Key: {}, URL: {}", s3Key, fileUrl);

            return fileUrl;
        } catch (IOException ex) {
            log.error("IOException during file upload", ex);
            throw new FileUploadException("Failed to read file content: " + ex.getMessage(), "FILE_READ_ERROR", ex);
        } catch (SdkException ex) {
            log.error("AWS S3 error during file upload", ex);
            throw new FileUploadException("Failed to upload file to S3: " + ex.getMessage(), "S3_UPLOAD_ERROR", ex);
        } catch (Exception ex) {
            log.error("Unexpected error during file upload", ex);
            throw new FileUploadException("File upload failed: " + ex.getMessage(), "UPLOAD_ERROR", ex);
        }
    }

    /**
     * Download file from S3
     */
    public byte[] downloadFile(String s3Key) {
        try {
            log.info("Downloading file from S3. Key: {}", s3Key);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            byte[] fileContent = s3Client.getObjectAsBytes(getObjectRequest).asByteArray();

            log.info("File downloaded successfully from S3. Key: {}", s3Key);
            return fileContent;

        } catch (NoSuchKeyException ex) {
            log.warn("File not found in S3. Key: {}", s3Key);
            throw new FileUploadException("File not found in S3", "FILE_NOT_FOUND", ex);
        } catch (SdkException ex) {
            log.error("AWS S3 error during file download", ex);
            throw new FileUploadException("Failed to download file from S3: " + ex.getMessage(), "S3_DOWNLOAD_ERROR", ex);
        }
    }

    /**
     * Delete file from S3
     */
    public void deleteFile(String s3Key) {
        try {
            log.info("Deleting file from S3. Key: {}", s3Key);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

            log.info("File deleted successfully from S3. Key: {}", s3Key);

        } catch (SdkException ex) {
            log.error("AWS S3 error during file deletion", ex);
            throw new FileUploadException("Failed to delete file from S3: " + ex.getMessage(), "S3_DELETE_ERROR", ex);
        }
    }

    /**
     * Check if file exists in S3
     */
    public boolean fileExists(String s3Key) {
        try {
            log.info("Checking if file exists in S3. Key: {}", s3Key);

            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;

        } catch (NoSuchKeyException ex) {
            log.warn("File does not exist in S3. Key: {}", s3Key);
            return false;
        } catch (SdkException ex) {
            log.error("AWS S3 error checking file existence", ex);
            throw new FileUploadException("Failed to check file in S3: " + ex.getMessage(), "S3_CHECK_ERROR", ex);
        }
    }

    /**
     * Get file metadata from S3
     */
    public Map<String, String> getFileMetadata(String s3Key) {
        try {
            log.info("Fetching file metadata from S3. Key: {}", s3Key);

            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            HeadObjectResponse response = s3Client.headObject(headObjectRequest);

            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("content-length", String.valueOf(response.contentLength()));
            metadata.put("content-type", response.contentType());
            metadata.put("last-modified", response.lastModified().toString());
            metadata.putAll(response.metadata());

            return metadata;

        } catch (NoSuchKeyException ex) {
            log.warn("File not found in S3. Key: {}", s3Key);
            throw new FileUploadException("File not found in S3", "FILE_NOT_FOUND", ex);
        } catch (SdkException ex) {
            log.error("AWS S3 error fetching metadata", ex);
            throw new FileUploadException("Failed to fetch file metadata: " + ex.getMessage(), "S3_METADATA_ERROR", ex);
        }
    }

    /**
     * Generate S3 file URL
     */
    public String generateS3Url(String s3Key) {
        // Format: https://bucket-name.s3.amazonaws.com/key
        return String.format("https://%s.s3.amazonaws.com/%s", bucketName, s3Key);
    }

    /**
     * Upload file with custom S3 key
     */
    public String uploadFileWithCustomKey(MultipartFile file, String customS3Key) {
        try {
            log.info("Uploading file with custom S3 key. File: {}, Key: {}",
                    file.getOriginalFilename(), customS3Key);

            if (file == null || file.isEmpty()) {
                throw new FileUploadException("File is empty or null", "EMPTY_FILE");
            }

            byte[] fileContent = file.getBytes();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(customS3Key)
                    .contentType(file.getContentType())
                    .contentLength((long) fileContent.length)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileContent));

            String fileUrl = generateS3Url(customS3Key);

            log.info("File uploaded successfully with custom key. Key: {}", customS3Key);
            return fileUrl;

        } catch (IOException ex) {
            log.error("IOException during file upload", ex);
            throw new FileUploadException("Failed to read file content", "FILE_READ_ERROR", ex);
        } catch (SdkException ex) {
            log.error("AWS S3 error during file upload", ex);
            throw new FileUploadException("Failed to upload file to S3", "S3_UPLOAD_ERROR", ex);
        }
    }
}
