package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-207-04 : accès à l'analyse de déclaration AT Fedris — 1 ligne par
 * {@code case_file_id} (unicité enforced en base).
 */
public interface AtFedrisDeclarationRepository
        extends JpaRepository<AtFedrisDeclarationAnalysis, UUID> {

    Optional<AtFedrisDeclarationAnalysis> findByCaseFileId(UUID caseFileId);
}
