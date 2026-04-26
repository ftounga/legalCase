package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PartageSuccessoralRepository extends JpaRepository<PartageSuccessoralAnalysis, UUID> {

    Optional<PartageSuccessoralAnalysis> findByCaseFileId(UUID caseFileId);
}
