package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HarcelementNulliteRepository extends JpaRepository<HarcelementNulliteAnalysis, UUID> {

    Optional<HarcelementNulliteAnalysis> findByCaseFileId(UUID caseFileId);
}
