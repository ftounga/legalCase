package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PpvExonerationRepository
        extends JpaRepository<PpvExonerationAnalysis, UUID> {

    Optional<PpvExonerationAnalysis> findByCaseFileId(UUID caseFileId);
}
