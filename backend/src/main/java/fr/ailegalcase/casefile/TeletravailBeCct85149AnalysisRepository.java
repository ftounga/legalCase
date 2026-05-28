package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-16 : accès JPA aux {@link TeletravailBeCct85149Analysis} —
 * unicité fonctionnelle sur {@code case_file_id} garantie par la
 * migration Liquibase (contrainte UNIQUE + index).
 */
public interface TeletravailBeCct85149AnalysisRepository
        extends JpaRepository<TeletravailBeCct85149Analysis, UUID> {

    Optional<TeletravailBeCct85149Analysis> findByCaseFileId(UUID caseFileId);
}
