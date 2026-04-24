package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DivorceAccepteRepository extends JpaRepository<DivorceAccepteAnalysis, UUID> {

    Optional<DivorceAccepteAnalysis> findByCaseFileId(UUID caseFileId);
}
