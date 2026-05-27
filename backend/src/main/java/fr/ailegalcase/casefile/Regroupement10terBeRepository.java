package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** SF-215-03 : repository de l'analyse regroupement familial 10ter BE. */
public interface Regroupement10terBeRepository
        extends JpaRepository<Regroupement10terBeAnalysis, UUID> {

    Optional<Regroupement10terBeAnalysis> findByCaseFileId(UUID caseFileId);
}
