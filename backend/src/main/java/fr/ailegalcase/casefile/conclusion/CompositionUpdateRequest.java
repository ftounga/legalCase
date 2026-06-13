package fr.ailegalcase.casefile.conclusion;

import java.util.List;

/**
 * F-288 — corps du {@code PUT .../conclusions/composition}.
 *
 * <p>Contrat : {@code { "dimensions": ["DECISION_TOOL", ...], "exclusions": [ {
 * "dimension": "DECISION_TOOL", "itemKey": "&lt;key&gt;" } ] } }.</p>
 *
 * <p>{@code dimensions} (SF-288-04) = les dimensions que le client gère
 * <strong>autoritairement</strong> et veut remettre à plat — y compris à
 * <strong>vide</strong> (delete sans réinsertion). Indispensable pour que « tout
 * recocher » efface réellement les exclusions précédentes : sinon un body
 * {@code exclusions: []} ne toucherait aucune dimension et l'exclusion resterait
 * collée. Si {@code dimensions} est absent (compat. SF-288-01), seules les dimensions
 * présentes dans {@code exclusions} sont remises à plat.</p>
 *
 * <p>Aucune logique dans le DTO (coding-rules).</p>
 */
public record CompositionUpdateRequest(List<String> dimensions, List<ExclusionEntry> exclusions) {

    /** Compat. SF-288-01 (appelants/tests sans {@code dimensions}). */
    public CompositionUpdateRequest(List<ExclusionEntry> exclusions) {
        this(null, exclusions);
    }

    public record ExclusionEntry(String dimension, String itemKey) {}
}
