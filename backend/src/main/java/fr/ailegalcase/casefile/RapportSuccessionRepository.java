package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RapportSuccessionRepository extends JpaRepository<RapportSuccessionAnalysis, UUID> {

    Optional<RapportSuccessionAnalysis> findByCaseFileId(UUID caseFileId);
}
