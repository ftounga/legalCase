package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-216-03 : repository JPA pour la pension alimentaire enfant FR
 * (art. 371-2 Cciv).
 */
public interface PensionAlimentaireEnfantFrRepository
        extends JpaRepository<PensionAlimentaireEnfantFrAnalysis, UUID> {

    Optional<PensionAlimentaireEnfantFrAnalysis> findByCaseFileId(UUID caseFileId);
}
