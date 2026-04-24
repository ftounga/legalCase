package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AesFamilleRepository extends JpaRepository<AesFamilleAnalysis, UUID> {

    Optional<AesFamilleAnalysis> findByCaseFileId(UUID caseFileId);
}
