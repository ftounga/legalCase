package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface Belgian40terRepository extends JpaRepository<Belgian40terAnalysis, UUID> {

    Optional<Belgian40terAnalysis> findByCaseFileId(UUID caseFileId);
}
