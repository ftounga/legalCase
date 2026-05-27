package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-02 : accès JPA aux {@link RccBeLongueCarriereAnalysis} — unicité
 * fonctionnelle sur {@code case_file_id} garantie par la migration Liquibase
 * (contrainte UNIQUE + index).
 */
public interface RccBeLongueCarriereAnalysisRepository
        extends JpaRepository<RccBeLongueCarriereAnalysis, UUID> {

    Optional<RccBeLongueCarriereAnalysis> findByCaseFileId(UUID caseFileId);
}
