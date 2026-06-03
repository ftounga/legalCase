package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** SF-221-03 : repository de l'analyse de statut résident longue durée UE (BE). */
public interface ResidenceLongueDureeUeBeRepository
        extends JpaRepository<ResidenceLongueDureeUeBeAnalysis, UUID> {

    Optional<ResidenceLongueDureeUeBeAnalysis> findByCaseFileId(UUID caseFileId);
}
