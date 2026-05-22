package fr.ailegalcase.jurisprudencemapping;

import java.util.List;

/**
 * F-JU-02 / SF-JU-02-01 — agrégation des arrêts mappés par outil utilisé sur
 * un dossier. Sert à la construction du prompt F-98 conclusions générées.
 *
 * @param toolId          identifiant de l'outil source
 * @param brancheCalculId branche de calcul active
 * @param citations       arrêts mappés (déjà filtrés ≤ 3 par
 *                        {@link ToolJurisprudenceService})
 */
public record ToolJurisprudenceCitationByTool(
        String toolId,
        String brancheCalculId,
        List<ToolJurisprudenceCitationResponse> citations) {
}
