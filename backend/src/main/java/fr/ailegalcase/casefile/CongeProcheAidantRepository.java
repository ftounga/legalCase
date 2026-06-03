package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CongeProcheAidantRepository
        extends JpaRepository<CongeProcheAidantAnalysis, UUID> {

    Optional<CongeProcheAidantAnalysis> findByCaseFileId(UUID caseFileId);
}
