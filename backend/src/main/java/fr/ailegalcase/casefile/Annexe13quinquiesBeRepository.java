package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** SF-215-17 : repository de l'analyse Annexe 13quinquies (OQT + interdiction d'entrée, BE). */
public interface Annexe13quinquiesBeRepository
        extends JpaRepository<Annexe13quinquiesBeAnalysis, UUID> {

    Optional<Annexe13quinquiesBeAnalysis> findByCaseFileId(UUID caseFileId);
}
