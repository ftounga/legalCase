package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RegimeMayotteRepository extends JpaRepository<RegimeMayotteAnalysis, UUID> {

    Optional<RegimeMayotteAnalysis> findByCaseFileId(UUID caseFileId);
}
