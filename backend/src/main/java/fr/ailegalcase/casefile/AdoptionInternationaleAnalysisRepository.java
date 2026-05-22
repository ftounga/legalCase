package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-216-17 : repository JPA pour les analyses Adoption internationale FR
 * (art. 370-3 à 370-5 Cciv + Convention La Haye 1993).
 */
public interface AdoptionInternationaleAnalysisRepository
        extends JpaRepository<AdoptionInternationaleAnalysis, UUID> {

    Optional<AdoptionInternationaleAnalysis> findByCaseFileId(UUID caseFileId);
}
