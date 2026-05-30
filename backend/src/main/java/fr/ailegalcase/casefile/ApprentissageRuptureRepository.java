package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApprentissageRuptureRepository
        extends JpaRepository<ApprentissageRuptureAnalysis, UUID> {

    Optional<ApprentissageRuptureAnalysis> findByCaseFileId(UUID caseFileId);
}
