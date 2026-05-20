package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-207-05 : accès à l'analyse du référé tribunal du travail BE — 1 ligne
 * par {@code case_file_id} (unicité enforced en base).
 */
public interface RefereTribunalTravailBeRepository
        extends JpaRepository<RefereTribunalTravailBeAnalysis, UUID> {

    Optional<RefereTribunalTravailBeAnalysis> findByCaseFileId(UUID caseFileId);
}
