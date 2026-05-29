package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VpfLiensPersonnelsRepository extends JpaRepository<VpfLiensPersonnelsAnalysis, UUID> {

    Optional<VpfLiensPersonnelsAnalysis> findByCaseFileId(UUID caseFileId);
}
