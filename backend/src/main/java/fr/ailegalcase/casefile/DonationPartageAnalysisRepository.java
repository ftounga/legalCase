package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-216-29 : repository JPA pour les analyses Donation-partage FR
 * (art. 1075 à 1075-5 Cciv).
 */
public interface DonationPartageAnalysisRepository
        extends JpaRepository<DonationPartageAnalysis, UUID> {

    Optional<DonationPartageAnalysis> findByCaseFileId(UUID caseFileId);
}
