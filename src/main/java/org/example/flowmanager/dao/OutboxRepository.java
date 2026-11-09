package org.example.flowmanager.dao;

import org.example.flowmanager.model.entity.Outbox;
import org.example.flowmanager.model.status.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    List<Outbox> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
