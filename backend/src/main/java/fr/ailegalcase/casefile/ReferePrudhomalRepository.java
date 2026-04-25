package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReferePrudhomalRepository extends JpaRepository<ReferePrudhomalAnalysis, UUID> {

    Optional<ReferePrudhomalAnalysis> findByCaseFileId(UUID caseFileId);
}
