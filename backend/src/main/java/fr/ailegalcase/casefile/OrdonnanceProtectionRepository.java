package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrdonnanceProtectionRepository extends JpaRepository<OrdonnanceProtectionAnalysis, UUID> {

    Optional<OrdonnanceProtectionAnalysis> findByCaseFileId(UUID caseFileId);
}
