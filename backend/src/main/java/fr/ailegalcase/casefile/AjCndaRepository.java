package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AjCndaRepository extends JpaRepository<AjCndaAnalysis, UUID> {

    Optional<AjCndaAnalysis> findByCaseFileId(UUID caseFileId);
}
