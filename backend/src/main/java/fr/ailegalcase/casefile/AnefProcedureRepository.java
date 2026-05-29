package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AnefProcedureRepository extends JpaRepository<AnefProcedureAnalysis, UUID> {

    Optional<AnefProcedureAnalysis> findByCaseFileId(UUID caseFileId);
}
