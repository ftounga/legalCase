package fr.ailegalcase.analysis;

import java.util.List;
import java.util.UUID;

/**
 * F-179 SF-179-01 — DTO de réponse de {@code GET .../jurisprudence-checks}.
 *
 * @param checks liste des vérifications de la dernière analyse {@code DONE} du
 *               dossier (vide si aucune référence détectée)
 */
public record JurisprudenceCheckResponse(List<Check> checks) {

    /**
     * Une vérification de référence jurisprudentielle.
     *
     * @param id               identifiant du check
     * @param documentName     nom du document où la référence a été détectée
     * @param reference        libellé canonique de la référence
     * @param statut           VERIFIED / SUSPECT / NOT_FOUND / UNCERTAIN
     * @param explication      explication courte du verdict
     * @param positionAlleguee position que le document prête à l'arrêt
     * @param sourceUrl        lien source (Légifrance / Juridat), ou {@code null}
     * @param claudeConfidence HIGH / MEDIUM / LOW, ou {@code null}
     * @param webSearchUsed    true si le fallback web search a été tenté
     */
    public record Check(
            UUID id,
            String documentName,
            String reference,
            String statut,
            String explication,
            String positionAlleguee,
            String sourceUrl,
            String claudeConfidence,
            boolean webSearchUsed) {
    }

    static JurisprudenceCheckResponse from(List<JurisprudenceCheck> entities) {
        List<Check> mapped = entities.stream()
                .map(c -> new Check(
                        c.getId(),
                        c.getDocumentName(),
                        c.getReference(),
                        c.getStatut() != null ? c.getStatut().name() : JurisprudenceCheckStatus.UNCERTAIN.name(),
                        c.getExplication(),
                        c.getPositionAlleguee(),
                        c.getSourceUrl(),
                        c.getClaudeConfidence(),
                        c.isWebSearchUsed()))
                .toList();
        return new JurisprudenceCheckResponse(mapped);
    }
}
