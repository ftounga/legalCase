package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.ProcedureStageCatalog;

import java.time.Instant;
import java.util.UUID;

/**
 * F-98 / SF-98-01 + SF-98-52 — corps de la réponse {@code 200} des routes de
 * lecture d'une version de conclusions ({@code GET .../conclusions},
 * {@code GET .../conclusions/versions/{versionId}}, {@code PATCH .../lifecycle}).
 *
 * <p>Contrat API figé. Quand aucune version {@code case_conclusions} n'existe pour le
 * dossier, le statut synthétique {@code NOT_GENERATED} est renvoyé et tous les champs
 * sauf {@code caseFileId} et {@code status} valent {@code null} ({@code versionNumber}
 * vaut {@code 0}).</p>
 *
 * @param status          l'un de {@code NOT_GENERATED | PENDING | PROCESSING | DONE | FAILED}
 * @param versionNumber   numéro de version (SF-98-52) ; {@code 0} si jamais généré
 * @param lifecycleStatus cycle de vie de la version (SF-98-52) ; {@code null} si jamais généré
 * @param styleApplied    SF-98-47 — vrai si la génération a adopté le style appris du
 *                        cabinet ; {@code false} pour une génération générique ou
 *                        quand aucune version n'existe ({@code NOT_GENERATED})
 * @param stale           SF-98-53 — vrai si la version est potentiellement périmée :
 *                        l'analyse du dossier ({@code CaseAnalysis} {@code DONE}) a
 *                        évolué depuis la génération de la version. {@code false} pour
 *                        une version non {@code DONE}, sans {@code generatedAt}, sans
 *                        analyse postérieure, ou quand aucune version n'existe. Calculé
 *                        à la lecture, jamais persisté.
 */
public record ConclusionResponse(
        UUID id,
        UUID caseFileId,
        int versionNumber,
        String status,
        String lifecycleStatus,
        String content,
        String jurisdictionLabel,
        String stageLabel,
        String positionLabel,
        String modelUsed,
        boolean styleApplied,
        boolean stale,
        Instant generatedAt,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt) {

    /** Statut synthétique renvoyé quand aucune génération n'a jamais été déclenchée. */
    public static final String NOT_GENERATED = "NOT_GENERATED";

    /** Réponse « jamais généré » — seuls {@code caseFileId} et {@code status} sont renseignés. */
    public static ConclusionResponse notGenerated(UUID caseFileId) {
        return new ConclusionResponse(null, caseFileId, 0, NOT_GENERATED, null,
                null, null, null, null, null, false, false, null, null, null, null);
    }

    /**
     * Construit la réponse à partir d'une version persistée, en résolvant les libellés
     * humains du stade procédural depuis {@link ProcedureStageCatalog}.
     *
     * @param conclusion la version {@code case_conclusions}
     * @param domain     domaine du dossier ({@code legalDomain})
     * @param country    pays du workspace
     * @param stale      SF-98-53 — péremption calculée par le service de lecture
     */
    public static ConclusionResponse from(CaseConclusion conclusion, String domain, String country,
                                          boolean stale) {
        return new ConclusionResponse(
                conclusion.getId(),
                conclusion.getCaseFile().getId(),
                conclusion.getVersionNumber(),
                conclusion.getStatus().name(),
                conclusion.getLifecycleStatus().name(),
                conclusion.getContent(),
                ProcedureStageCatalog.jurisdictionLabel(domain, country, conclusion.getJurisdictionCode()),
                ProcedureStageCatalog.stageLabel(domain, country, conclusion.getStageCode()),
                ProcedureStageCatalog.positionLabel(domain, country, conclusion.getPositionCode()),
                conclusion.getModelUsed(),
                conclusion.isStyleApplied(),
                stale,
                conclusion.getGeneratedAt(),
                conclusion.getErrorMessage(),
                conclusion.getCreatedAt(),
                conclusion.getUpdatedAt());
    }

    /**
     * Variante de {@link #from} tolérante : si la résolution des libellés échoue (ex. codes
     * hors catalogue d'un domaine/pays inattendu), les libellés tombent à {@code null} sans
     * faire échouer la réponse.
     *
     * @param stale SF-98-53 — péremption calculée par le service de lecture
     */
    public static ConclusionResponse fromSafe(CaseConclusion conclusion, CaseFile caseFile, String country,
                                              boolean stale) {
        try {
            return from(conclusion, caseFile.getLegalDomain(), country, stale);
        } catch (RuntimeException ex) {
            return new ConclusionResponse(
                    conclusion.getId(), caseFile.getId(), conclusion.getVersionNumber(),
                    conclusion.getStatus().name(), conclusion.getLifecycleStatus().name(),
                    conclusion.getContent(), null, null, null,
                    conclusion.getModelUsed(), conclusion.isStyleApplied(), stale,
                    conclusion.getGeneratedAt(), conclusion.getErrorMessage(),
                    conclusion.getCreatedAt(), conclusion.getUpdatedAt());
        }
    }
}
