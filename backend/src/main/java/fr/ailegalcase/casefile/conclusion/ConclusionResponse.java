package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.ProcedureStageCatalog;

import java.time.Instant;
import java.util.UUID;

/**
 * F-98 / SF-98-01 — corps de la réponse {@code 200} du {@code GET .../conclusions}.
 *
 * <p>Contrat API figé. Quand aucune ligne {@code case_conclusions} n'existe pour le
 * dossier, le statut synthétique {@code NOT_GENERATED} est renvoyé et tous les champs
 * sauf {@code caseFileId} et {@code status} valent {@code null}.</p>
 *
 * @param status l'un de {@code NOT_GENERATED | PENDING | PROCESSING | DONE | FAILED}
 */
public record ConclusionResponse(
        UUID id,
        UUID caseFileId,
        String status,
        String content,
        String jurisdictionLabel,
        String stageLabel,
        String positionLabel,
        String modelUsed,
        Instant generatedAt,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt) {

    /** Statut synthétique renvoyé quand aucune génération n'a jamais été déclenchée. */
    public static final String NOT_GENERATED = "NOT_GENERATED";

    /** Réponse « jamais généré » — seuls {@code caseFileId} et {@code status} sont renseignés. */
    public static ConclusionResponse notGenerated(UUID caseFileId) {
        return new ConclusionResponse(null, caseFileId, NOT_GENERATED,
                null, null, null, null, null, null, null, null, null);
    }

    /**
     * Construit la réponse à partir d'une ligne persistée, en résolvant les libellés
     * humains du stade procédural depuis {@link ProcedureStageCatalog}.
     *
     * @param conclusion la ligne {@code case_conclusions}
     * @param domain     domaine du dossier ({@code legalDomain})
     * @param country    pays du workspace
     */
    public static ConclusionResponse from(CaseConclusion conclusion, String domain, String country) {
        return new ConclusionResponse(
                conclusion.getId(),
                conclusion.getCaseFile().getId(),
                conclusion.getStatus().name(),
                conclusion.getContent(),
                ProcedureStageCatalog.jurisdictionLabel(domain, country, conclusion.getJurisdictionCode()),
                ProcedureStageCatalog.stageLabel(domain, country, conclusion.getStageCode()),
                ProcedureStageCatalog.positionLabel(domain, country, conclusion.getPositionCode()),
                conclusion.getModelUsed(),
                conclusion.getGeneratedAt(),
                conclusion.getErrorMessage(),
                conclusion.getCreatedAt(),
                conclusion.getUpdatedAt());
    }

    /**
     * Variante de {@link #from} tolérante : si la résolution des libellés échoue (ex. codes
     * hors catalogue d'un domaine/pays inattendu), les libellés tombent à {@code null} sans
     * faire échouer la réponse.
     */
    public static ConclusionResponse fromSafe(CaseConclusion conclusion, CaseFile caseFile, String country) {
        try {
            return from(conclusion, caseFile.getLegalDomain(), country);
        } catch (RuntimeException ex) {
            return new ConclusionResponse(
                    conclusion.getId(), caseFile.getId(), conclusion.getStatus().name(),
                    conclusion.getContent(), null, null, null,
                    conclusion.getModelUsed(), conclusion.getGeneratedAt(), conclusion.getErrorMessage(),
                    conclusion.getCreatedAt(), conclusion.getUpdatedAt());
        }
    }
}
