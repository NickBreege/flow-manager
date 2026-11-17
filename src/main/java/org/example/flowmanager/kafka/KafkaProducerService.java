package org.example.flowmanager.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowmanager.event.FileConversionRequestedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {
    private final KafkaTemplate<String, FileConversionRequestedEvent> kafkaTemplate;

    @Value("${app.kafka.topics.requested}")
    private String requestedTopic;

    public void send(FileConversionRequestedEvent event) {
        try {
            kafkaTemplate.send(requestedTopic, event.eventId().toString(), event).get();
            log.info("Событие {} успешно отправлено в Kafka. Топик: {}", event.eventId(), requestedTopic);

        } catch (Exception e) {
            log.error("Ошибка отправки события {} в Kafka. Топик: {}", event.eventId(), requestedTopic, e);
            throw new RuntimeException("Ошибка отправки события в Kafka", e);
        }
    }
}
