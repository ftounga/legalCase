package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TeletravailAccordRepository
        extends JpaRepository<TeletravailAccordAnalysis, UUID> {

    Optional<TeletravailAccordAnalysis> findByCaseFileId(UUID caseFileId);
}
