package fr.ailegalcase.casefile;

import java.util.List;

/**
 * F-262 SF-262-01 — Réponse de l'endpoint
 * {@code GET /api/v1/case-files/{caseFileId}/heads-of-claim}.
 *
 * <p>Ne contient que les chefs de demande <strong>applicables</strong> au
 * dossier. Pour chacun, {@code addressed} indique s'il est couvert (chef outillé
 * calculé) ou seulement rappelé (chef transverse, ou outillé non calculé).</p>
 *
 * @param heads liste des chefs applicables (jamais {@code null}).
 */
public record HeadsOfClaimResponse(List<HeadItem> heads) {

    /**
     * Item de réponse pour un chef applicable.
     *
     * @param code      identifiant stable du chef.
     * @param label     libellé court.
     * @param fondement fondement juridique.
     * @param category  {@code INDEMNITAIRE_OUTILLE} | {@code TRANSVERSE}.
     * @param toolId    identifiant de l'outil mappé, ou {@code null} (transverse).
     * @param addressed {@code true} si le chef est couvert (outil calculé) ;
     *                  {@code false} sinon (rappel).
     */
    public record HeadItem(
            String code,
            String label,
            String fondement,
            String category,
            String toolId,
            boolean addressed
    ) {
    }
}
