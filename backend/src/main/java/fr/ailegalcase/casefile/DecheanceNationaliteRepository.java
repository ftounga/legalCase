package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DecheanceNationaliteRepository extends JpaRepository<DecheanceNationaliteAnalysis, UUID> {

    Optional<DecheanceNationaliteAnalysis> findByCaseFileId(UUID caseFileId);
}
