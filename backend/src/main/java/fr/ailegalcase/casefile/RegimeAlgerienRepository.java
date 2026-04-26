package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RegimeAlgerienRepository extends JpaRepository<RegimeAlgerienAnalysis, UUID> {

    Optional<RegimeAlgerienAnalysis> findByCaseFileId(UUID caseFileId);
}
