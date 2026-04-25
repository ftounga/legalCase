package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PseRepository extends JpaRepository<PseAnalysis, UUID> {

    Optional<PseAnalysis> findByCaseFileId(UUID caseFileId);
}
