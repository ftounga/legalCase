package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DivorceDdiBeRepository extends JpaRepository<DivorceDdiBeAnalysis, UUID> {

    Optional<DivorceDdiBeAnalysis> findByCaseFileId(UUID caseFileId);
}
