package org.example.flowmanager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.example.flowmanager.dao.OutboxRepository;
import org.example.flowmanager.event.FileConversionRequestedEvent;
import org.example.flowmanager.kafka.KafkaProducerService;
import org.example.flowmanager.model.entity.Outbox;
import org.example.flowmanager.model.status.OutboxStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.example.flowmanager.model.status.OutboxStatus.NEW;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {
    private final OutboxRepository outboxRepository;
    private final KafkaProducerService kafkaProducerService;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    @SchedulerLock(name = "flowmanager-outbox-publisher", lockAtMostFor = "10m", lockAtLeastFor = "1s")
    public void publishNewEvents() {
        List<Outbox> events = outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(NEW);

        for (Outbox outbox : events) {
            publishEvent(outbox);
        }
    }

    private void publishEvent(Outbox outbox) {
        try {
            FileConversionRequestedEvent event = objectMapper.readValue(
                    outbox.getPayload(), FileConversionRequestedEvent.class);

            kafkaProducerService.send(event);

            outbox.setStatus(OutboxStatus.PUBLISHED);
            outbox.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));

            outboxRepository.save(outbox);

            log.info("Событие {} опубликовано в Kafka", outbox.getEventId());

        } catch (Exception e) {
            log.error("Ошибка публикации события {} в Kafka. Событие останется в Outbox для повторной попытки",
                    outbox.getEventId(), e);
        }
    }
}
