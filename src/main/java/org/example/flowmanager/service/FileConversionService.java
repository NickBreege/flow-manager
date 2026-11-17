package org.example.flowmanager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowmanager.dao.FileConversionRepository;
import org.example.flowmanager.dao.InboxRepository;
import org.example.flowmanager.dao.OutboxRepository;
import org.example.flowmanager.event.FileConversionCompletedEvent;
import org.example.flowmanager.event.FileConversionRequestedEvent;
import org.example.flowmanager.exception.ConversionNotReadyException;
import org.example.flowmanager.model.entity.FileConversion;
import org.example.flowmanager.model.entity.Inbox;
import org.example.flowmanager.model.entity.Outbox;
import org.example.flowmanager.model.status.ConversionStatus;
import org.example.flowmanager.model.status.InboxStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.example.flowmanager.event.FileConversionEventType.FILE_CONVERSION_REQUESTED;
import static org.example.flowmanager.model.status.OutboxStatus.NEW;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileConversionService {
    private final MinioService minioService;
    private final FileConversionRepository fileConversionRepository;
    private final OutboxRepository outboxRepository;
    private final InboxRepository inboxRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public FileConversion createConversion(MultipartFile file) {
        String originalFilePath = minioService.upload(file);

        try {
            return transactionTemplate.execute(status -> {
                UUID eventId = UUID.randomUUID();

                FileConversion fileConversion = new FileConversion();
                fileConversion.setEventId(eventId);
                fileConversion.setOriginalFilePath(originalFilePath);
                fileConversion.setStatus(ConversionStatus.PROCESSING);

                FileConversion savedConversion = fileConversionRepository.save(fileConversion);

                FileConversionRequestedEvent event = new FileConversionRequestedEvent(
                        eventId,
                        FILE_CONVERSION_REQUESTED.name(),
                        OffsetDateTime.now(ZoneOffset.UTC),
                        new FileConversionRequestedEvent.Payload(originalFilePath)
                );

                saveToOutbox(event);

                log.info("Конвертация и событие Outbox сохранены. conversionId={}, eventId={}",
                        savedConversion.getId(),
                        event.eventId());

                return savedConversion;
            });

        } catch (RuntimeException e) {
            log.error("Не удалось сохранить конвертацию и событие Outbox. Удаляем исходный файл из MinIO. filePath={}",
                    originalFilePath, e);

            try {
                minioService.delete(originalFilePath);
            } catch (RuntimeException deleteException) {
                log.error("Не удалось удалить файл из MinIO после ошибки сохранения в БД. filePath={}",
                        originalFilePath, deleteException);

                e.addSuppressed(deleteException);
            }
            throw e;
        }
    }

    public FileConversion getConversion(Long id) {
        return fileConversionRepository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("Конвертация с ID " + id + " не найдена"));
    }

    public String getConvertedFilePath(Long id) {
        FileConversion conversion = getConversion(id);

        if (conversion.getStatus() == ConversionStatus.FAILED) {
            throw new ConversionNotReadyException("Конвертация завершилась с ошибкой. conversionId=" + id);
        }

        if (conversion.getStatus() != ConversionStatus.SUCCESS) {
            throw new ConversionNotReadyException("Конвертация ещё не завершена. conversionId=" + id);
        }

        String filePath = conversion.getConvertedFilePath();

        if (filePath == null || filePath.isBlank()) {
            throw new IllegalStateException("Путь к готовому PDF отсутствует. conversionId=" + id);
        }

        return filePath;
    }

    public void processCompletedEvent(FileConversionCompletedEvent event) {
        transactionTemplate.executeWithoutResult(status -> {
            Inbox inbox = inboxRepository.findByEventId(event.eventId())
                    .orElseGet(() -> createInbox(event.eventId()));

            if (inbox.getStatus() == InboxStatus.SUCCESS) {
                log.info("Событие уже обработано, пропускаем. eventId={}", event.eventId());
                return;
            }

            FileConversion conversion = fileConversionRepository.findByEventId(event.eventId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Конвертация для события не найдена. eventId=" + event.eventId()));

            if (event.payload().errorMessage() != null && !event.payload().errorMessage().isBlank()) {
                conversion.setStatus(ConversionStatus.FAILED);
                log.warn("Конвертация завершилась с ошибкой. conversionId={}, eventId={}, error={}",
                        conversion.getId(), event.eventId(), event.payload().errorMessage());
            } else {
                conversion.setConvertedFilePath(event.payload().filePath());
                conversion.setStatus(ConversionStatus.SUCCESS);
                log.info("Конвертация завершена успешно. conversionId={}, eventId={}, filePath={}",
                        conversion.getId(), event.eventId(), event.payload().filePath());
            }

            fileConversionRepository.save(conversion);

            inbox.setStatus(InboxStatus.SUCCESS);
            inbox.setProcessedAt(LocalDateTime.now(ZoneOffset.UTC));
            inboxRepository.save(inbox);
        });
    }

    private void saveToOutbox(FileConversionRequestedEvent event) {
        Outbox outbox = new Outbox();

        outbox.setEventId(event.eventId());
        outbox.setEventType(event.eventType());
        try {
            outbox.setPayload(objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось сериализовать событие Outbox: " + event.eventId(), e);
        }
        outbox.setStatus(NEW);
        outbox.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));

        outboxRepository.save(outbox);

        log.info("Событие {} сохранено в Outbox", event.eventId());
    }

    private Inbox createInbox(UUID eventId) {
        Inbox inbox = new Inbox();

        inbox.setEventId(eventId);
        inbox.setStatus(InboxStatus.RECEIVED);
        inbox.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));

        return inboxRepository.save(inbox);
    }
}
