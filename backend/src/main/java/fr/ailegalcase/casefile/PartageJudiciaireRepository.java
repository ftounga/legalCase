package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PartageJudiciaireRepository extends JpaRepository<PartageJudiciaireAnalysis, UUID> {

    Optional<PartageJudiciaireAnalysis> findByCaseFileId(UUID caseFileId);
}
