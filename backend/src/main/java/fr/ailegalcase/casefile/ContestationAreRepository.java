package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ContestationAreRepository extends JpaRepository<ContestationAreAnalysis, UUID> {

    Optional<ContestationAreAnalysis> findByCaseFileId(UUID caseFileId);
}
