package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AutoriteParentaleRepository extends JpaRepository<AutoriteParentaleAnalysis, UUID> {

    Optional<AutoriteParentaleAnalysis> findByCaseFileId(UUID caseFileId);
}
