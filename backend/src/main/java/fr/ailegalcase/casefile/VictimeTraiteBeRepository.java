package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** SF-221-06 : repository de l'analyse du titre victime de la traite des êtres humains (BE). */
public interface VictimeTraiteBeRepository
        extends JpaRepository<VictimeTraiteBeAnalysis, UUID> {

    Optional<VictimeTraiteBeAnalysis> findByCaseFileId(UUID caseFileId);
}
