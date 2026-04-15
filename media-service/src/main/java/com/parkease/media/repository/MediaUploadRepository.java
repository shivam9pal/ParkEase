package com.parkease.media.repository;

import com.parkease.media.entity.MediaUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaUploadRepository extends JpaRepository<MediaUpload, UUID> {

    Optional<MediaUpload> findByIdAndDeletedAtIsNull(UUID id);

    @Query("SELECT m FROM MediaUpload m WHERE m.userId = :userId AND m.deletedAt IS NULL")
    List<MediaUpload> findByUserIdAndNotDeleted(@Param("userId") UUID userId);

    @Query("SELECT m FROM MediaUpload m WHERE m.fileType = :fileType AND m.deletedAt IS NULL")
    List<MediaUpload> findByFileType(@Param("fileType") String fileType);

    @Query("SELECT m FROM MediaUpload m WHERE m.referenceId = :referenceId AND m.deletedAt IS NULL")
    List<MediaUpload> findByReferenceId(@Param("referenceId") String referenceId);

    @Query("SELECT m FROM MediaUpload m WHERE m.userId = :userId AND m.fileType = :fileType AND m.deletedAt IS NULL")
    List<MediaUpload> findByUserIdAndFileType(@Param("userId") UUID userId, @Param("fileType") String fileType);

    @Query("SELECT m FROM MediaUpload m WHERE m.s3Key = :s3Key AND m.deletedAt IS NULL")
    Optional<MediaUpload> findByS3Key(@Param("s3Key") String s3Key);

    @Query("SELECT m FROM MediaUpload m WHERE m.uploadStatus = 'FAILED' AND m.deletedAt IS NULL")
    List<MediaUpload> findFailedUploads();
}
