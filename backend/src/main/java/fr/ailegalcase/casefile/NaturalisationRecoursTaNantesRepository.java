package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NaturalisationRecoursTaNantesRepository
        extends JpaRepository<NaturalisationRecoursTaNantesAnalysis, UUID> {

    Optional<NaturalisationRecoursTaNantesAnalysis> findByCaseFileId(UUID caseFileId);
}
