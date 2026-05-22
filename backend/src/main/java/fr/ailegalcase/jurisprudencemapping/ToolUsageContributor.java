package fr.ailegalcase.jurisprudencemapping;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-02 / SF-JU-02-01 — interface implémentée par chaque outil décisionnel
 * pour déclarer si / comment il est utilisé sur un dossier donné.
 *
 * <p>Le {@link ToolUsageAggregator} interroge tous les beans de ce type lors
 * de la génération de conclusions F-98 ; chaque outil retourne
 * {@code Optional<ToolUsage>} non-vide si l'avocat a effectivement rempli /
 * calculé ce dossier dans cet outil, et avec quelle branche de calcul active.</p>
 *
 * <p>V1 : aucun outil ne l'implémente encore → le détecteur retourne vide,
 * comportement neutre (aucune injection dans le prompt F-98).</p>
 */
public interface ToolUsageContributor {

    /** Identifiant de l'outil (matching {@code TOOL_REGISTRY}). */
    String toolId();

    /**
     * Retourne l'usage sur ce dossier si l'outil a été utilisé, sinon
     * {@link Optional#empty()}. Jamais {@code null}.
     */
    Optional<ToolUsage> detectUsage(UUID caseFileId);
}
