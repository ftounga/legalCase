package fr.ailegalcase.analysis;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

/**
 * F-192 SF-192-01 — Alignement matérialisé d'une piste stratégique RETAINED
 * d'une analyse vers un outil décisionnel cible (TOOL_REGISTRY frontend).
 *
 * <p>Sérialisé en JSON dans
 * {@code case_analyses.retained_pistes_alignment_json} par
 * {@link RetainedPisteAlignmentService#materializeForAnalysis(CaseAnalysis)}.</p>
 *
 * @param pisteId         identifiant de la piste (clone sur la nouvelle analyse)
 * @param texte           texte libre de la piste
 * @param baseJuridique   référence légale extraite ({@code null} possible)
 * @param horizonTemporel horizon ({@code null} possible)
 * @param conditions      conditions extraites de la piste
 * @param toolIdCible     id TOOL_REGISTRY frontend ({@code null} si {@code NO_TARGET_TOOL})
 * @param matchStatus     {@code ALIGNED} / {@code DIVERGENT} / {@code NOT_ANALYZED}
 *                        / {@code NO_TARGET_TOOL}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RetainedPisteAlignment(
        UUID pisteId,
        String texte,
        String baseJuridique,
        String horizonTemporel,
        List<String> conditions,
        String toolIdCible,
        String matchStatus
) {
    public static final String STATUS_ALIGNED = "ALIGNED";
    public static final String STATUS_DIVERGENT = "DIVERGENT";
    public static final String STATUS_NOT_ANALYZED = "NOT_ANALYZED";
    public static final String STATUS_NO_TARGET_TOOL = "NO_TARGET_TOOL";
}
