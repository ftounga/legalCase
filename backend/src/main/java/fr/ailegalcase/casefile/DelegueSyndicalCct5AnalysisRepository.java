package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-10 : accès JPA aux {@link DelegueSyndicalCct5Analysis} —
 * unicité fonctionnelle sur {@code case_file_id} garantie par la
 * migration Liquibase (contrainte UNIQUE + index).
 */
public interface DelegueSyndicalCct5AnalysisRepository
        extends JpaRepository<DelegueSyndicalCct5Analysis, UUID> {

    Optional<DelegueSyndicalCct5Analysis> findByCaseFileId(UUID caseFileId);
}
