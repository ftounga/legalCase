package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-217-04 : repository de l'analyse d'autorité parentale belge (1:1 par dossier).
 */
public interface AutoriteParentaleBeRepository extends JpaRepository<AutoriteParentaleBeAnalysis, UUID> {

    Optional<AutoriteParentaleBeAnalysis> findByCaseFileId(UUID caseFileId);
}
