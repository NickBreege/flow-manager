package org.example.flowmanager.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.flowmanager.model.status.ConversionStatus;

import java.util.UUID;

@Entity
@Table(name = "file_conversions")
@Getter
@Setter
@NoArgsConstructor
public class FileConversion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID eventId;

    @Column(nullable = false)
    private String originalFilePath;

    private String convertedFilePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversionStatus status;
}

