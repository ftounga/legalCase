package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MotifGraveBeRepository extends JpaRepository<MotifGraveBeAnalysis, UUID> {

    Optional<MotifGraveBeAnalysis> findByCaseFileId(UUID caseFileId);
}
