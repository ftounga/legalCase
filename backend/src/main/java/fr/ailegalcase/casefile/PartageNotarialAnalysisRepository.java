package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-216-27 : repository JPA pour les analyses Partage successoral
 * notarié FR (art. 816 et s. Cciv).
 */
public interface PartageNotarialAnalysisRepository
        extends JpaRepository<PartageNotarialAnalysis, UUID> {

    Optional<PartageNotarialAnalysis> findByCaseFileId(UUID caseFileId);
}
