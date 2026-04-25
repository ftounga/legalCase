package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LicenciementEconomiqueRepository extends JpaRepository<LicenciementEconomiqueAnalysis, UUID> {

    Optional<LicenciementEconomiqueAnalysis> findByCaseFileId(UUID caseFileId);
}
