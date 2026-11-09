package org.example.flowmanager.service;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {
    private final MinioClient minioClient;
    @Value("${app.minio.bucket}")
    private String bucketName;

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не должен быть пустым");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "file";
        }

        String filePath = UUID.randomUUID() + "_" + originalFilename;

        String contentType = file.getContentType();

        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }
        log.info("Начало загрузки файла в бакет. bucket={}, filePath={}", bucketName, filePath);

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(filePath)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(contentType)
                    .build());

            log.info("Файл успешно загружен в бакет. bucket={}, filePath={}", bucketName, filePath);
            return filePath;

        } catch (Exception e) {
            log.error("Ошибка загрузки файла в бакет. bucket={}, filePath={}", bucketName, filePath, e);
            throw new RuntimeException("Не удалось загрузить файл в MinIO: " + filePath, e);
        }
    }

    public void delete(String filePath) {
        log.info("Начало удаления файла из бакета. bucket={}, filePath={}", bucketName, filePath);

        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(filePath)
                    .build());

            log.info("Файл успешно удалён из бакета. bucket={}, filePath={}", bucketName, filePath);

        } catch (Exception e) {
            log.error("Ошибка удаления файла. bucket={}, filePath={}", bucketName, filePath, e);

            throw new RuntimeException("Не удалось удалить файл из MinIO: " + filePath, e);
        }
    }

    public InputStream download(String bucket, String filePath) {
        try {
            log.info("Начало скачивания файла. bucket={}, filePath={}", bucket, filePath);

            GetObjectResponse inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(filePath)
                            .build()
            );

            log.info("Поток файла успешно получен. bucket={}, filePath={}", bucket, filePath);

            return inputStream;


        } catch (Exception e) {
            log.error("Ошибка скачивания файла. bucket={}, filePath={}", bucket, filePath, e);

            throw new IllegalStateException("Не удалось скачать файл из MinIO: " + filePath, e);
        }
    }

    public InputStream download(String filePath) {
        return download(bucketName, filePath);
    }
}
