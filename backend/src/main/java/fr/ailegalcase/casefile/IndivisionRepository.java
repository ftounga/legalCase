package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IndivisionRepository extends JpaRepository<IndivisionAnalysis, UUID> {

    Optional<IndivisionAnalysis> findByCaseFileId(UUID caseFileId);
}
