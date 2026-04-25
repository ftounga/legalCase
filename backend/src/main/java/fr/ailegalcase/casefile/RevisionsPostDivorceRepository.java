package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RevisionsPostDivorceRepository extends JpaRepository<RevisionsPostDivorceAnalysis, UUID> {

    Optional<RevisionsPostDivorceAnalysis> findByCaseFileId(UUID caseFileId);
}
