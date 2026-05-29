package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppelCaaCassationRepository extends JpaRepository<AppelCaaCassationAnalysis, UUID> {

    Optional<AppelCaaCassationAnalysis> findByCaseFileId(UUID caseFileId);
}
