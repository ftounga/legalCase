package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InaptitudeRepository extends JpaRepository<InaptitudeAnalysis, UUID> {

    Optional<InaptitudeAnalysis> findByCaseFileId(UUID caseFileId);
}
