package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExecutionJugementCphRepository
        extends JpaRepository<ExecutionJugementCphAnalysis, UUID> {

    Optional<ExecutionJugementCphAnalysis> findByCaseFileId(UUID caseFileId);
}
