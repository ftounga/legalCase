package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PourvoiCassationSocRepository
        extends JpaRepository<PourvoiCassationSocAnalysis, UUID> {

    Optional<PourvoiCassationSocAnalysis> findByCaseFileId(UUID caseFileId);
}
