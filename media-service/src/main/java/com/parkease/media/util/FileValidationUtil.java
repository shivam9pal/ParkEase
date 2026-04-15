package com.parkease.media.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
public class FileValidationUtil {

    @Value("${file.upload.max-size}")
    private long maxFileSize;

    @Value("${file.upload.allowed-extensions}")
    private String allowedExtensionsString;

    @Value("${file.upload.allowed-types}")
    private String allowedMimeTypesString;

    private static final Set<String> ALLOWED_FILE_TYPES = new HashSet<>(Arrays.asList(
            "PROFILE_PIC", "LOT_IMAGE", "RECEIPT", "VEHICLE_DOC"
    ));

    public boolean isValidFileType(String fileType) {
        return fileType != null && ALLOWED_FILE_TYPES.contains(fileType.toUpperCase());
    }

    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }

        // Check file size
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds max allowed size of " + maxFileSize + " bytes");
        }

        // Check file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("File name is invalid");
        }

        String fileExtension = getFileExtension(originalFilename).toLowerCase();
        Set<String> allowedExtensions = new HashSet<>(Arrays.asList(allowedExtensionsString.split(",")));

        if (!allowedExtensions.contains(fileExtension)) {
            throw new IllegalArgumentException("File extension not allowed. Allowed types: " + allowedExtensionsString);
        }

        // Check MIME type
        String mimeType = file.getContentType();
        if (mimeType == null || mimeType.isEmpty()) {
            throw new IllegalArgumentException("File MIME type is invalid");
        }

        Set<String> allowedMimeTypes = new HashSet<>(Arrays.asList(allowedMimeTypesString.split(",")));
        if (!allowedMimeTypes.contains(mimeType)) {
            throw new IllegalArgumentException("File MIME type not allowed. Allowed types: " + allowedMimeTypesString);
        }
    }

    public String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot + 1);
        }
        return "";
    }

    public String getSafeFileName(String originalFileName) {
        // Remove path separators and special characters
        return originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public String generateS3Key(String fileType, String userId, String originalFileName) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String fileExtension = getFileExtension(originalFileName);
        String safeFileName = getSafeFileName(originalFileName).replaceAll("\\." + fileExtension + "$", "");

        return String.format("%s/%s_%s_%s.%s",
                fileType.toLowerCase(),
                userId,
                timestamp,
                safeFileName,
                fileExtension);
    }
}
