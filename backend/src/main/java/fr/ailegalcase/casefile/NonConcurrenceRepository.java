package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NonConcurrenceRepository
        extends JpaRepository<NonConcurrenceAnalysis, UUID> {

    Optional<NonConcurrenceAnalysis> findByCaseFileId(UUID caseFileId);
}
