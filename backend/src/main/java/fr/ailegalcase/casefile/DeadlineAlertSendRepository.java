package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DeadlineAlertSendRepository extends JpaRepository<DeadlineAlertSend, UUID> {

    boolean existsByDeadlineIdAndThresholdDays(UUID deadlineId, int thresholdDays);
}
