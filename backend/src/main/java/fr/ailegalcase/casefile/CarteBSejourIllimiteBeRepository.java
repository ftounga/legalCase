package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** SF-221-02 : repository de l'analyse de passage carte A → carte B (séjour illimité BE). */
public interface CarteBSejourIllimiteBeRepository
        extends JpaRepository<CarteBSejourIllimiteBeAnalysis, UUID> {

    Optional<CarteBSejourIllimiteBeAnalysis> findByCaseFileId(UUID caseFileId);
}
