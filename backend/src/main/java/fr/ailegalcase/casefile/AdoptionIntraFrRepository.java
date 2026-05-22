package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-216-15 : repository JPA pour les analyses Adoption intra-familiale FR
 * (art. 343-1 al. 2 + 345-1 Cciv).
 */
public interface AdoptionIntraFrRepository
        extends JpaRepository<AdoptionIntraFrAnalysis, UUID> {

    Optional<AdoptionIntraFrAnalysis> findByCaseFileId(UUID caseFileId);
}
