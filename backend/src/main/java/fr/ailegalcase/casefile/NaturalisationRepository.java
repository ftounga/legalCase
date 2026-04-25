package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NaturalisationRepository extends JpaRepository<NaturalisationAnalysis, UUID> {

    Optional<NaturalisationAnalysis> findByCaseFileId(UUID caseFileId);
}
