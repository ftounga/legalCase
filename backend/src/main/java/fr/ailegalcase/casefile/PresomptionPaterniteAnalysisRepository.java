package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-216-25 : repository JPA pour les analyses Présomption de paternité FR
 * (art. 312-315 Cciv + art. 316 al. 2 + art. 333 al. 1).
 */
public interface PresomptionPaterniteAnalysisRepository
        extends JpaRepository<PresomptionPaterniteAnalysis, UUID> {

    Optional<PresomptionPaterniteAnalysis> findByCaseFileId(UUID caseFileId);
}
