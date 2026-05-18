package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-217-08 : repository de l'analyse de pension alimentaire entre ex-époux belge
 * (1:1 par dossier).
 */
public interface ContributionConjointBeRepository
        extends JpaRepository<ContributionConjointBeAnalysis, UUID> {

    Optional<ContributionConjointBeAnalysis> findByCaseFileId(UUID caseFileId);
}
