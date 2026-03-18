package fr.ailegalcase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UsageEventRepository extends JpaRepository<UsageEvent, UUID> {
    List<UsageEvent> findByCaseFileIdOrderByCreatedAtDesc(UUID caseFileId);
    List<UsageEvent> findByCaseFileIdIn(Collection<UUID> caseFileIds);
}
