package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CasePhaseRepository extends JpaRepository<CasePhase, UUID> {

    /**
     * Phases d'un dossier ordonnées par date d'entrée. Le tri secondaire
     * déterministe (par ordre de référentiel {@link CasePhaseType#getOrder()})
     * est appliqué dans le service, car l'enum est persisté en STRING.
     */
    List<CasePhase> findByCaseFileIdOrderByEnteredAtAsc(UUID caseFileId);
}
