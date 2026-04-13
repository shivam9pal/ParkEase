package com.parkease.media.repository;

import com.parkease.media.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaRepository extends JpaRepository<Media, UUID> {

    Optional<Media> findByIdAndIsDeletedFalse(UUID id);

    List<Media> findByFolderAndIsDeletedFalse(String folder);

    List<Media> findByReferenceAndIsDeletedFalse(String reference);

    List<Media> findByUploadedByAndIsDeletedFalse(String uploadedBy);

    Optional<Media> findByS3Key(String s3Key);
}
