package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SinglePermitBeRepository extends JpaRepository<SinglePermitBeAnalysis, UUID> {

    Optional<SinglePermitBeAnalysis> findByCaseFileId(UUID caseFileId);
}
