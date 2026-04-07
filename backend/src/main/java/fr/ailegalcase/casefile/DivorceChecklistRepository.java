package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface DivorceChecklistRepository extends JpaRepository<DivorceChecklist, UUID> {
    Optional<DivorceChecklist> findByCaseFileId(UUID caseFileId);
}
