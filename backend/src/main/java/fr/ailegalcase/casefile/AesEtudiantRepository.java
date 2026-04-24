package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AesEtudiantRepository extends JpaRepository<AesEtudiantAnalysis, UUID> {

    Optional<AesEtudiantAnalysis> findByCaseFileId(UUID caseFileId);
}
