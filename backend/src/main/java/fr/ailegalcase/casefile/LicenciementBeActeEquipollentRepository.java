package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-213-09 : accès JPA aux {@link LicenciementBeActeEquipollentAnalysis} —
 * unicité fonctionnelle sur {@code case_file_id} garantie par la migration
 * Liquibase (contrainte UNIQUE + index).
 */
public interface LicenciementBeActeEquipollentRepository
        extends JpaRepository<LicenciementBeActeEquipollentAnalysis, UUID> {

    Optional<LicenciementBeActeEquipollentAnalysis> findByCaseFileId(UUID caseFileId);
}
