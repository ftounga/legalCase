package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AesMetiersTensionRepository extends JpaRepository<AesMetiersTensionAnalysis, UUID> {

    Optional<AesMetiersTensionAnalysis> findByCaseFileId(UUID caseFileId);
}
