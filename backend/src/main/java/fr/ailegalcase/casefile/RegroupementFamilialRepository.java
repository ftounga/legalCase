package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RegroupementFamilialRepository extends JpaRepository<RegroupementFamilialAnalysis, UUID> {

    Optional<RegroupementFamilialAnalysis> findByCaseFileId(UUID caseFileId);
}
