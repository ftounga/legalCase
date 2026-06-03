package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** SF-221-01 : repository de l'analyse de prorogation de la carte A BE. */
public interface CarteAProrogationBeRepository
        extends JpaRepository<CarteAProrogationBeAnalysis, UUID> {

    Optional<CarteAProrogationBeAnalysis> findByCaseFileId(UUID caseFileId);
}
