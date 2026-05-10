package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AcceptationRenonciationSuccessionRepository
        extends JpaRepository<AcceptationRenonciationSuccessionAnalysis, UUID> {

    Optional<AcceptationRenonciationSuccessionAnalysis> findByCaseFileId(UUID caseFileId);
}
