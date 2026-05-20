package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResiliationJudiciaireCphRepository
        extends JpaRepository<ResiliationJudiciaireCphAnalysis, UUID> {

    Optional<ResiliationJudiciaireCphAnalysis> findByCaseFileId(UUID caseFileId);
}
