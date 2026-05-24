package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DemissionEquivoqueRepository
        extends JpaRepository<DemissionEquivoqueAnalysis, UUID> {

    Optional<DemissionEquivoqueAnalysis> findByCaseFileId(UUID caseFileId);
}
