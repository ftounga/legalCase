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
 * Dimensions applicables, dans l'ordre : {@code DECISION_TOOL} (vague 1, outils calculés)
 * puis {@code ADVERSE_MOYEN} (SF-288-03, moyens adverses persistés — omise si aucun moyen).
 * {@code included = false} si la clé de l'item est dans l'ensemble d'exclusions persisté de
 * sa dimension, sinon {@code true}.</p>
 */
public record CompositionResponse(List<Dimension> dimensions) {

    public record Dimension(String key, String label, List<Item> items) {}

    public record Item(String key, String label, boolean included) {}
}
