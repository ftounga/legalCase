package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SuccessionBeAcceptationRenonciationRepository
        extends JpaRepository<SuccessionBeAcceptationRenonciationAnalysis, UUID> {

    Optional<SuccessionBeAcceptationRenonciationAnalysis> findByCaseFileId(UUID caseFileId);
}
