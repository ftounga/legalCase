package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OfpraIntroductionRepository extends JpaRepository<OfpraIntroductionAnalysis, UUID> {

    Optional<OfpraIntroductionAnalysis> findByCaseFileId(UUID caseFileId);
}
