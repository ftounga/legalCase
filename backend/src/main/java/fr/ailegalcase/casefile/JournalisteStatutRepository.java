package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JournalisteStatutRepository
        extends JpaRepository<JournalisteStatutAnalysis, UUID> {

    Optional<JournalisteStatutAnalysis> findByCaseFileId(UUID caseFileId);
}
