package org.example.flowmanager.dao;

import org.example.flowmanager.model.entity.FileConversion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FileConversionRepository extends JpaRepository<FileConversion, Long> {
    Optional<FileConversion> findByEventId(UUID eventId);
}
