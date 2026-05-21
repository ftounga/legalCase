package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-216-05 : repository JPA pour la liquidation de communauté légale FR
 * (art. 1467-1517 Cciv).
 */
public interface LiquidationCommunauteFrRepository
        extends JpaRepository<LiquidationCommunauteFrAnalysis, UUID> {

    Optional<LiquidationCommunauteFrAnalysis> findByCaseFileId(UUID caseFileId);
}
