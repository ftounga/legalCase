package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-32 : accès JPA aux
 * {@link InterruptionCarriereSoinsParentalAnalysis} — unicité
 * fonctionnelle sur {@code case_file_id} garantie par la migration
 * Liquibase (contrainte UNIQUE + index).
 */
public interface InterruptionCarriereSoinsParentalAnalysisRepository
        extends JpaRepository<InterruptionCarriereSoinsParentalAnalysis, UUID> {

    Optional<InterruptionCarriereSoinsParentalAnalysis> findByCaseFileId(
            UUID caseFileId);
}
