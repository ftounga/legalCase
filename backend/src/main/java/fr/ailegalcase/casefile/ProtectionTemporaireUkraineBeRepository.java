package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** SF-215-19 : repository de l'analyse Protection temporaire Ukraine BE (F-IM-34). */
public interface ProtectionTemporaireUkraineBeRepository
        extends JpaRepository<ProtectionTemporaireUkraineBeAnalysis, UUID> {

    Optional<ProtectionTemporaireUkraineBeAnalysis> findByCaseFileId(UUID caseFileId);
}
