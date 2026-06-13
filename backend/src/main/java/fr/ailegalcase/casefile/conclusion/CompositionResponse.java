package fr.ailegalcase.casefile.conclusion;

import java.util.List;

/**
 * F-288 / SF-288-01 — réponse des endpoints de composition des conclusions.
 *
 * <p>Contrat figé (GET et PUT renvoient ce même corps) :
 * <pre>
 * { "dimensions": [ { "key": "DECISION_TOOL", "label": "Outils décisionnels",
 *     "items": [ { "key": "&lt;toolId&gt;", "label": "&lt;titre&gt;", "included": true|false } ] } ] }
 * </pre>
 * Vague 1 : exactement UNE dimension {@code DECISION_TOOL}. {@code included = false}
 * si le {@code toolId} est dans l'ensemble d'exclusions persisté, sinon {@code true}.</p>
 */
public record CompositionResponse(List<Dimension> dimensions) {

    public record Dimension(String key, String label, List<Item> items) {}

    public record Item(String key, String label, boolean included) {}
}
