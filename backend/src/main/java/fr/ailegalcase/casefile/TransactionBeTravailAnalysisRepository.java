package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-213-06 : accès JPA aux {@link TransactionBeTravailAnalysis} — unicité
 * fonctionnelle sur {@code case_file_id} garantie par la migration
 * Liquibase (contrainte UNIQUE + index).
 */
public interface TransactionBeTravailAnalysisRepository
        extends JpaRepository<TransactionBeTravailAnalysis, UUID> {

    Optional<TransactionBeTravailAnalysis> findByCaseFileId(UUID caseFileId);
}
