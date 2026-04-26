package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AsileAvanceRepository extends JpaRepository<AsileAvanceAnalysis, UUID> {

    Optional<AsileAvanceAnalysis> findByCaseFileId(UUID caseFileId);
}
