package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-28 : accès JPA aux {@link MpFedrisReconnaissanceAnalysis} —
 * unicité fonctionnelle sur {@code case_file_id} garantie par la
 * migration Liquibase (contrainte UNIQUE + index).
 */
public interface MpFedrisReconnaissanceAnalysisRepository
        extends JpaRepository<MpFedrisReconnaissanceAnalysis, UUID> {

    Optional<MpFedrisReconnaissanceAnalysis> findByCaseFileId(UUID caseFileId);
}
