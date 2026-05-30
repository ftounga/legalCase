package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SaisieRemunerationRepository
        extends JpaRepository<SaisieRemunerationAnalysis, UUID> {

    Optional<SaisieRemunerationAnalysis> findByCaseFileId(UUID caseFileId);
}
