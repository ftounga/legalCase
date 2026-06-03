package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RegimeAlgerienBeRepository
        extends JpaRepository<RegimeAlgerienBeAnalysis, UUID> {

    Optional<RegimeAlgerienBeAnalysis> findByCaseFileId(UUID caseFileId);
}
