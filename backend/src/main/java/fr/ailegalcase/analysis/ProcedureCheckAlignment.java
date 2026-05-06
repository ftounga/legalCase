package fr.ailegalcase.analysis;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

/**
 * F-193 SF-193-01 — Alignement matérialisé d'un point procédural F-96 d'une
 * analyse vers un outil décisionnel cible (TOOL_REGISTRY frontend).
 *
 * <p>Sérialisé en JSON dans
 * {@code case_analyses.procedure_checks_alignment_json} par
 * {@link ProcedureCheckAlignmentService#materializeForAnalysis(CaseAnalysis)}.</p>
 *
 * <p>Pattern miroir {@link RetainedPisteAlignment} (F-192) — le statut
 * F-96 ({@link ProcedureCheckStatus}) est plus riche que la trichotomie F-176
 * (RETAINED/DISCARDED/TO_STUDY) donc {@code matchStatus} a 6 valeurs au lieu
 * de 4.</p>
 *
 * @param checkId        identifiant du check (clone sur la nouvelle analyse)
 * @param libelle        libellé du check (description F-96)
 * @param critereCode    code surveillé (peut être {@code null} pour les
 *                       checks libres sans mapping)
 * @param statut         statut F-96 actuel ({@link ProcedureCheckStatus})
 * @param expectedValue  valeur attendue pour les énumérés F-IA-03 ({@code null}
 *                       pour les binaires)
 * @param raison         raison libre saisie par l'avocat (statut
 *                       NON_COMPLIANT / requalification ; {@code null} sinon)
 * @param toolIdCible    id TOOL_REGISTRY frontend ({@code null} si
 *                       {@code NO_TARGET_TOOL})
 * @param matchStatus    {@code ALIGNED} / {@code DIVERGENT} /
 *                       {@code NON_COMPLIANT_FLAG} / {@code TO_VERIFY_FLAG} /
 *                       {@code NOT_ANALYZED} / {@code NO_TARGET_TOOL}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProcedureCheckAlignment(
        UUID checkId,
        String libelle,
        String critereCode,
        String statut,
        String expectedValue,
        String raison,
        String toolIdCible,
        String matchStatus
) {
    public static final String STATUS_ALIGNED = "ALIGNED";
    public static final String STATUS_DIVERGENT = "DIVERGENT";
    public static final String STATUS_NON_COMPLIANT_FLAG = "NON_COMPLIANT_FLAG";
    public static final String STATUS_TO_VERIFY_FLAG = "TO_VERIFY_FLAG";
    public static final String STATUS_NOT_ANALYZED = "NOT_ANALYZED";
    public static final String STATUS_NO_TARGET_TOOL = "NO_TARGET_TOOL";
}
