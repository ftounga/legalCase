package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ElectionsCseConformiteRepository
        extends JpaRepository<ElectionsCseConformiteAnalysis, UUID> {

    Optional<ElectionsCseConformiteAnalysis> findByCaseFileId(UUID caseFileId);
}
