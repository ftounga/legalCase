package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** SF-221-04 : repository de l'analyse de détention en centre fermé + requête mise en liberté (BE). */
public interface DetentionCentreFermeBeRepository
        extends JpaRepository<DetentionCentreFermeBeAnalysis, UUID> {

    Optional<DetentionCentreFermeBeAnalysis> findByCaseFileId(UUID caseFileId);
}
