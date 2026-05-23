package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-216-13 : repository JPA pour les analyses Audition du mineur par le
 * JAF FR (art. 388-1 Cciv).
 */
public interface AuditionMineurAnalysisRepository
        extends JpaRepository<AuditionMineurAnalysis, UUID> {

    Optional<AuditionMineurAnalysis> findByCaseFileId(UUID caseFileId);
}
