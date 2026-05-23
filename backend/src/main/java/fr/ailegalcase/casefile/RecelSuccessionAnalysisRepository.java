package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-216-21 : repository JPA pour les analyses Recel successoral FR
 * (art. 778 Cciv).
 */
public interface RecelSuccessionAnalysisRepository
        extends JpaRepository<RecelSuccessionAnalysis, UUID> {

    Optional<RecelSuccessionAnalysis> findByCaseFileId(UUID caseFileId);
}
