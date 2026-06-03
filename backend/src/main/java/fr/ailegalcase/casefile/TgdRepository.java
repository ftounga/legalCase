package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-222-02 : repository JPA pour l'analyse d'éligibilité TGD (art. 41-3-1 CPP).
 */
public interface TgdRepository extends JpaRepository<TgdAnalysis, UUID> {

    Optional<TgdAnalysis> findByCaseFileId(UUID caseFileId);
}
