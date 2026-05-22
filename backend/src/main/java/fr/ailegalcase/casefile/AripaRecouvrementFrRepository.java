package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-216-07 : repository JPA pour les analyses ARIPA recouvrement FR
 * (art. L. 581+ CSS).
 */
public interface AripaRecouvrementFrRepository
        extends JpaRepository<AripaRecouvrementFrAnalysis, UUID> {

    Optional<AripaRecouvrementFrAnalysis> findByCaseFileId(UUID caseFileId);
}
