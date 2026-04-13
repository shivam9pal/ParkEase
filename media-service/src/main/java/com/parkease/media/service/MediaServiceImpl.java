package com.parkease.media.service;

import com.parkease.media.dto.MediaResponse;
import com.parkease.media.dto.UploadRequest;
import com.parkease.media.dto.UploadResponse;
import com.parkease.media.entity.Media;
import com.parkease.media.exception.FileValidationException;
import com.parkease.media.exception.MediaServiceException;
import com.parkease.media.repository.MediaRepository;
import com.parkease.media.util.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaServiceImpl implements MediaService {

    private final S3Service s3Service;
    private final MediaRepository mediaRepository;
    private final FileValidator fileValidator;

    @Override
    @Transactional
    public UploadResponse uploadFile(MultipartFile file, UploadRequest request) {
        try {
            // Validate file
            fileValidator.validate(file);

            // Generate S3 key
            String s3Key = fileValidator.generateS3Key(request.getFolder(), request.getReference(), file.getOriginalFilename());

            // Upload to S3
            String s3Url = s3Service.uploadFile(s3Key, file.getBytes(), file.getContentType());

            // Generate presigned URL
            String presignedUrl = s3Service.generatePresignedUrl(s3Key);

            // Save metadata to database
            Media media = Media.builder()
                    .fileName(file.getOriginalFilename())
                    .s3Key(s3Key)
                    .s3Url(s3Url)
                    .mimeType(file.getContentType())
                    .fileSize(file.getSize())
                    .folder(request.getFolder())
                    .uploadedBy(request.getUploadedBy())
                    .uploadedAt(LocalDateTime.now())
                    .description(request.getDescription())
                    .isDeleted(false)
                    .reference(request.getReference())
                    .build();

            Media savedMedia = mediaRepository.save(media);

            log.info("File uploaded successfully: {} -> S3Key: {}", file.getOriginalFilename(), s3Key);

            return UploadResponse.builder()
                    .mediaId(savedMedia.getId())
                    .fileName(savedMedia.getFileName())
                    .s3Url(savedMedia.getS3Url())
                    .presignedUrl(presignedUrl)
                    .fileSize(savedMedia.getFileSize())
                    .mimeType(savedMedia.getMimeType())
                    .uploadedAt(savedMedia.getUploadedAt())
                    .build();

        } catch (FileValidationException e) {
            log.warn("File validation failed: {}", e.getMessage());
            throw e;
        } catch (IOException e) {
            log.error("Error reading file content: {}", e.getMessage(), e);
            throw new MediaServiceException("Failed to read file content: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during file upload: {}", e.getMessage(), e);
            throw new MediaServiceException("Unexpected error during file upload: " + e.getMessage(), e);
        }
    }

    @Override
    public MediaResponse getMediaById(UUID mediaId) {
        Media media = mediaRepository.findByIdAndIsDeletedFalse(mediaId)
                .orElseThrow(() -> new MediaServiceException("Media not found: " + mediaId));

        return mapToResponse(media);
    }

    @Override
    public List<MediaResponse> getMediaByFolder(String folder) {
        List<Media> mediaList = mediaRepository.findByFolderAndIsDeletedFalse(folder);
        return mediaList.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<MediaResponse> getMediaByReference(String reference) {
        List<Media> mediaList = mediaRepository.findByReferenceAndIsDeletedFalse(reference);
        return mediaList.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteMedia(UUID mediaId) {
        Media media = mediaRepository.findByIdAndIsDeletedFalse(mediaId)
                .orElseThrow(() -> new MediaServiceException("Media not found: " + mediaId));

        media.setIsDeleted(true);
        media.setDeletedAt(LocalDateTime.now());
        mediaRepository.save(media);

        log.info("Media soft deleted: {} (S3Key: {})", mediaId, media.getS3Key());
    }

    @Override
    @Transactional
    public void hardDeleteMedia(UUID mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaServiceException("Media not found: " + mediaId));

        // Delete from S3
        s3Service.deleteFile(media.getS3Key());

        // Delete from database
        mediaRepository.delete(media);

        log.info("Media hard deleted: {} (S3Key: {})", mediaId, media.getS3Key());
    }

    @Override
    public String generatePresignedUrl(UUID mediaId) {
        Media media = mediaRepository.findByIdAndIsDeletedFalse(mediaId)
                .orElseThrow(() -> new MediaServiceException("Media not found: " + mediaId));

        return s3Service.generatePresignedUrl(media.getS3Key());
    }

    private MediaResponse mapToResponse(Media media) {
        return MediaResponse.builder()
                .id(media.getId())
                .fileName(media.getFileName())
                .s3Url(media.getS3Url())
                .mimeType(media.getMimeType())
                .fileSize(media.getFileSize())
                .folder(media.getFolder())
                .uploadedBy(media.getUploadedBy())
                .uploadedAt(media.getUploadedAt())
                .description(media.getDescription())
                .reference(media.getReference())
                .build();
    }
}
