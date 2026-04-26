package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IndivisionSuccessoraleRepository extends JpaRepository<IndivisionSuccessoraleAnalysis, UUID> {

    Optional<IndivisionSuccessoraleAnalysis> findByCaseFileId(UUID caseFileId);
}
