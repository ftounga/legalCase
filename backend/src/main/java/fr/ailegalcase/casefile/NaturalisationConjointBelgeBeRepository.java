package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** SF-215-09 : repository de l'analyse naturalisation conjoint Belge BE (art. 16 CNB). */
public interface NaturalisationConjointBelgeBeRepository
        extends JpaRepository<NaturalisationConjointBelgeBeAnalysis, UUID> {

    Optional<NaturalisationConjointBelgeBeAnalysis> findByCaseFileId(UUID caseFileId);
}
