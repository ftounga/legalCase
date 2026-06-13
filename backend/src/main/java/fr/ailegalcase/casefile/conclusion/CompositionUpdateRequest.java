package fr.ailegalcase.casefile.conclusion;

import java.util.List;

/**
 * F-288 / SF-288-01 — corps du {@code PUT .../conclusions/composition}.
 *
 * <p>Contrat figé : {@code { "exclusions": [ { "dimension": "DECISION_TOOL",
 * "itemKey": "&lt;toolId&gt;" } ] } }. Remplace l'ensemble d'exclusions des dimensions
 * présentes (delete + insert). Aucune logique dans le DTO (coding-rules).</p>
 */
public record CompositionUpdateRequest(List<ExclusionEntry> exclusions) {

    public record ExclusionEntry(String dimension, String itemKey) {}
}
