package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-14 : accès JPA aux {@link InterimBeCct322Analysis} — unicité
 * fonctionnelle sur {@code case_file_id} garantie par la migration
 * Liquibase (contrainte UNIQUE + index).
 */
public interface InterimBeCct322AnalysisRepository
        extends JpaRepository<InterimBeCct322Analysis, UUID> {

    Optional<InterimBeCct322Analysis> findByCaseFileId(UUID caseFileId);
}
