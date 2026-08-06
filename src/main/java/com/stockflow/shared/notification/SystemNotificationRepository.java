package com.stockflow.shared.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SystemNotificationRepository extends JpaRepository<SystemNotification, Long> {

    List<SystemNotification> findByUsernameOrderByReadFlagAscUpdatedAtDesc(String username);

    List<SystemNotification> findByUsername(String username);

    Optional<SystemNotification> findByIdAndUsername(Long id, String username);

    long countByUsernameAndReadFlagFalse(String username);
}
