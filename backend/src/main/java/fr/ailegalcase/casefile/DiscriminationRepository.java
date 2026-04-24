package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DiscriminationRepository extends JpaRepository<DiscriminationAnalysis, UUID> {

    Optional<DiscriminationAnalysis> findByCaseFileId(UUID caseFileId);
}
