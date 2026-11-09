package org.example.flowmanager.dao;

import org.example.flowmanager.model.entity.Inbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InboxRepository extends JpaRepository<Inbox, Long> {
    Optional<Inbox> findByEventId(UUID eventId);
}
