package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-216-23 : repository JPA pour les analyses Donation entre époux FR
 * (art. 1091-1100 Cciv).
 */
public interface DonationEntreEpouxAnalysisRepository
        extends JpaRepository<DonationEntreEpouxAnalysis, UUID> {

    Optional<DonationEntreEpouxAnalysis> findByCaseFileId(UUID caseFileId);
}
