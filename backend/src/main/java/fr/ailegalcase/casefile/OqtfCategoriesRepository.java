package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OqtfCategoriesRepository extends JpaRepository<OqtfCategoriesAnalysis, UUID> {

    Optional<OqtfCategoriesAnalysis> findByCaseFileId(UUID caseFileId);
}
