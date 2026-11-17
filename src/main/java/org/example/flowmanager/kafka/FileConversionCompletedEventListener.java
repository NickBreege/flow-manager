package org.example.flowmanager.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowmanager.event.FileConversionCompletedEvent;
import org.example.flowmanager.service.FileConversionService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileConversionCompletedEventListener {
    private final FileConversionService fileConversionService;

    @KafkaListener(topics = "${app.kafka.topics.completed}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(FileConversionCompletedEvent event) {
        log.info("Получено событие завершения конвертации. eventId={}", event.eventId());
        fileConversionService.processCompletedEvent(event);
    }
}
