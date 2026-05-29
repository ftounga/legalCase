package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RenouvellementDelaiRepository extends JpaRepository<RenouvellementDelaiAnalysis, UUID> {

    Optional<RenouvellementDelaiAnalysis> findByCaseFileId(UUID caseFileId);
}
