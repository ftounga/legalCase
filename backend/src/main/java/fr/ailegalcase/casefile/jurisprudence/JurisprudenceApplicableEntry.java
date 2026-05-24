package fr.ailegalcase.casefile.jurisprudence;

import java.util.List;

/**
 * F-JU-02 / SF-JU-02-02 — entrée de la liste « Jurisprudence applicable »
 * exposée par l'endpoint
 * {@code GET /api/v1/case-files/{id}/jurisprudence-applicable}.
 *
 * @param toolId          identifiant de l'outil source ayant été utilisé sur ce dossier
 * @param brancheCalculId branche de calcul active au moment de la détection d'usage
 * @param citations       arrêts mappés à la branche (≤ 3 par outil, dédupliqués
 *                        par {@code arret_ref} au niveau du contexte global)
 */
public record JurisprudenceApplicableEntry(
        String toolId,
        String brancheCalculId,
        List<JurisprudenceCitationDto> citations) {
}
