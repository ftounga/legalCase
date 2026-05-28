package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-27 : accès JPA aux
 * {@link InastriStatutTravailleurIndependantAnalysis} — unicité
 * fonctionnelle sur {@code case_file_id} garantie par la migration
 * Liquibase (contrainte UNIQUE + index).
 */
public interface InastriStatutTravailleurIndependantAnalysisRepository
        extends JpaRepository<InastriStatutTravailleurIndependantAnalysis, UUID> {

    Optional<InastriStatutTravailleurIndependantAnalysis> findByCaseFileId(
            UUID caseFileId);
}
