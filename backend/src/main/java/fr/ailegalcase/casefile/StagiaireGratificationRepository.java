package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StagiaireGratificationRepository
        extends JpaRepository<StagiaireGratificationAnalysis, UUID> {

    Optional<StagiaireGratificationAnalysis> findByCaseFileId(UUID caseFileId);
}
