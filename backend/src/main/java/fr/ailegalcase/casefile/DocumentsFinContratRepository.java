package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentsFinContratRepository
        extends JpaRepository<DocumentsFinContratAnalysis, UUID> {

    Optional<DocumentsFinContratAnalysis> findByCaseFileId(UUID caseFileId);
}
