package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-29 : acces JPA aux {@link AtMpRenteCapitalBeAnalysis} —
 * unicite fonctionnelle sur {@code case_file_id} garantie par la
 * migration Liquibase (contrainte UNIQUE + index).
 */
public interface AtMpRenteCapitalBeAnalysisRepository
        extends JpaRepository<AtMpRenteCapitalBeAnalysis, UUID> {

    Optional<AtMpRenteCapitalBeAnalysis> findByCaseFileId(UUID caseFileId);
}
