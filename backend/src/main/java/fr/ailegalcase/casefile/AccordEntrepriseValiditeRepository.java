package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccordEntrepriseValiditeRepository
        extends JpaRepository<AccordEntrepriseValiditeAnalysis, UUID> {

    Optional<AccordEntrepriseValiditeAnalysis> findByCaseFileId(UUID caseFileId);
}
