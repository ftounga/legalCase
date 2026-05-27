package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** SF-215-07 : repository de l'analyse naturalisation 12bis BE. */
public interface Naturalisation12bisBeRepository
        extends JpaRepository<Naturalisation12bisBeAnalysis, UUID> {

    Optional<Naturalisation12bisBeAnalysis> findByCaseFileId(UUID caseFileId);
}
