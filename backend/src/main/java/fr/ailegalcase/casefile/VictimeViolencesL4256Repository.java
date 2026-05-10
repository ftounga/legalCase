package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VictimeViolencesL4256Repository
        extends JpaRepository<VictimeViolencesL4256Analysis, UUID> {

    Optional<VictimeViolencesL4256Analysis> findByCaseFileId(UUID caseFileId);
}
