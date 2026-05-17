package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProcedureNulliteLicenciementRepository
        extends JpaRepository<ProcedureNulliteLicenciementAnalysis, UUID> {

    Optional<ProcedureNulliteLicenciementAnalysis> findByCaseFileId(UUID caseFileId);
}
