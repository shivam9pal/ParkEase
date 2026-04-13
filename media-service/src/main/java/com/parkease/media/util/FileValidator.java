package com.parkease.media.util;

import com.parkease.media.exception.FileValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
public class FileValidator {

    @Value("${media.upload.max-file-size}")
    private Long maxFileSize;

    @Value("${media.upload.allowed-types}")
    private String allowedTypes;

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("File is empty or null");
        }

        validateFileSize(file);
        validateMimeType(file);
    }

    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > maxFileSize) {
            throw new FileValidationException(
                    "File size (" + file.getSize() + " bytes) exceeds maximum allowed size (" + maxFileSize + " bytes)"
            );
        }
    }

    private void validateMimeType(MultipartFile file) {
        Set<String> allowed = new HashSet<>(Arrays.asList(allowedTypes.split(",")));
        String contentType = file.getContentType();

        if (contentType == null || !allowed.contains(contentType.trim())) {
            throw new FileValidationException(
                    "File type '" + contentType + "' is not allowed. Allowed types: " + allowedTypes
            );
        }
    }

    public String generateS3Key(String folder, String reference, String fileName) {
        return folder + "/" + reference + "/" + System.currentTimeMillis() + "_" + fileName;
    }
}
