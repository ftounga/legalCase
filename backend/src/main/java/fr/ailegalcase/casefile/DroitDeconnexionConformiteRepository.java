package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DroitDeconnexionConformiteRepository
        extends JpaRepository<DroitDeconnexionConformiteAnalysis, UUID> {

    Optional<DroitDeconnexionConformiteAnalysis> findByCaseFileId(UUID caseFileId);
}
