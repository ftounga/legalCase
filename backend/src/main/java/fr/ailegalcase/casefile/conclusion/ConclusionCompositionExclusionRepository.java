package fr.ailegalcase.casefile.conclusion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * F-288 / SF-288-01 — accès aux exclusions de composition d'un dossier.
 */
public interface ConclusionCompositionExclusionRepository
        extends JpaRepository<ConclusionCompositionExclusion, UUID> {

    List<ConclusionCompositionExclusion> findByCaseFileIdAndDimension(UUID caseFileId, String dimension);

    @Transactional
    void deleteByCaseFileIdAndDimension(UUID caseFileId, String dimension);
}
