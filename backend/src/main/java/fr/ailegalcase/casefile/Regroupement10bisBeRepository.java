package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** SF-215-05 : repository de l'analyse regroupement familial 10bis BE. */
public interface Regroupement10bisBeRepository
        extends JpaRepository<Regroupement10bisBeAnalysis, UUID> {

    Optional<Regroupement10bisBeAnalysis> findByCaseFileId(UUID caseFileId);
}
