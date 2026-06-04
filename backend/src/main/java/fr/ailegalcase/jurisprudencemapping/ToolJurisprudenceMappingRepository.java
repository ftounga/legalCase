package fr.ailegalcase.jurisprudencemapping;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * F-JU-01 / SF-JU-01-01 — accès aux {@link ToolJurisprudenceMapping}.
 */
public interface ToolJurisprudenceMappingRepository extends JpaRepository<ToolJurisprudenceMapping, UUID> {

    /**
     * SF-JU-01-FIX (2026-06-03) — citations « affichables » pour une branche d'un
     * outil : non archivées, <strong>chapeau officiel non vide</strong> et
     * <strong>confiance ≥ {@code minConfidence}</strong>, triées
     * {@code confidence_score DESC, date_arret DESC}. Le filtre qualité est
     * appliqué AVANT la limite (via {@code Pageable}) pour ne pas gâcher un slot
     * du top-N avec une citation vide ou peu fiable. Aligné sur l'invariant
     * « silence &gt; erreur » : ne jamais servir de citation vide / douteuse à
     * l'avocat (l'affichage outil F-JU-01 et les conclusions générées F-JU-02
     * partagent ce point de lecture via {@code ToolJurisprudenceService}).
     */
    @Query("""
            SELECT m FROM ToolJurisprudenceMapping m
            WHERE m.toolId = :toolId
              AND m.brancheCalculId = :brancheCalculId
              AND m.archived = false
              AND m.chapeauOfficiel IS NOT NULL
              AND TRIM(m.chapeauOfficiel) <> ''
              AND m.confidenceScore >= :minConfidence
            ORDER BY m.confidenceScore DESC, m.dateArret DESC
            """)
    List<ToolJurisprudenceMapping> findDisplayableByToolAndBranch(
            @Param("toolId") String toolId,
            @Param("brancheCalculId") String brancheCalculId,
            @Param("minConfidence") BigDecimal minConfidence,
            Pageable pageable);

    /**
     * SF-JU-01-14 — vérifie si un mapping existe déjà pour le triplet couvert par
     * la contrainte unique {@code uq_tool_jurisprudence_mappings_active}. Utilisé
     * par {@code JurisprudenceBootstrapService} pour rendre le bootstrap idempotent :
     * un re-bootstrap sur des entrées déjà mappées n'émet plus de
     * {@code DataIntegrityViolationException}.
     */
    boolean existsByToolIdAndBrancheCalculIdAndArretRef(String toolId, String brancheCalculId, String arretRef);

    /**
     * SF-JU-06-02 — tous les mappings actifs (non archivés), pour la
     * ré-évaluation/archivage de l'existant via {@code JurisprudenceReevaluationService}.
     */
    List<ToolJurisprudenceMapping> findByArchivedFalse();
}
