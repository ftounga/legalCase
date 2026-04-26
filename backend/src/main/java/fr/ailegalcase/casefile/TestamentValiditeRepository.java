package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TestamentValiditeRepository extends JpaRepository<TestamentValiditeAnalysis, UUID> {

    Optional<TestamentValiditeAnalysis> findByCaseFileId(UUID caseFileId);
}
