package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EgaliteSalarialeFhRepository
        extends JpaRepository<EgaliteSalarialeFhAnalysis, UUID> {

    Optional<EgaliteSalarialeFhAnalysis> findByCaseFileId(UUID caseFileId);
}
