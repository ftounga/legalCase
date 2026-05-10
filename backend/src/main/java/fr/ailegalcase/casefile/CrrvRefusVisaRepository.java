package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CrrvRefusVisaRepository extends JpaRepository<CrrvRefusVisaAnalysis, UUID> {

    Optional<CrrvRefusVisaAnalysis> findByCaseFileId(UUID caseFileId);
}
