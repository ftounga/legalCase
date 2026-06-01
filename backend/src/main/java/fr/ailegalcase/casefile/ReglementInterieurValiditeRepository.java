package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReglementInterieurValiditeRepository
        extends JpaRepository<ReglementInterieurValiditeAnalysis, UUID> {

    Optional<ReglementInterieurValiditeAnalysis> findByCaseFileId(UUID caseFileId);
}
