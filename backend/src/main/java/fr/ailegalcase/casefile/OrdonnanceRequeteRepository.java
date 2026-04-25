package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrdonnanceRequeteRepository extends JpaRepository<OrdonnanceRequeteAnalysis, UUID> {

    Optional<OrdonnanceRequeteAnalysis> findByCaseFileId(UUID caseFileId);
}
