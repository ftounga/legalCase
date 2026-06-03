package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** SF-221-05 : repository de l'analyse du recours CCE en suspension ordinaire (BE). */
public interface CceSuspensionBeRepository
        extends JpaRepository<CceSuspensionBeAnalysis, UUID> {

    Optional<CceSuspensionBeAnalysis> findByCaseFileId(UUID caseFileId);
}
