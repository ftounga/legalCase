package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ActionGroupeDiscriminationRepository
        extends JpaRepository<ActionGroupeDiscriminationAnalysis, UUID> {

    Optional<ActionGroupeDiscriminationAnalysis> findByCaseFileId(UUID caseFileId);
}
