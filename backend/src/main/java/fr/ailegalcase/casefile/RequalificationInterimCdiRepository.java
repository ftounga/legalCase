package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RequalificationInterimCdiRepository extends JpaRepository<RequalificationInterimCdiAnalysis, UUID> {

    Optional<RequalificationInterimCdiAnalysis> findByCaseFileId(UUID caseFileId);
}
