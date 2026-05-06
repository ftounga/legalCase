package fr.ailegalcase.analysis;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * F-195 SF-195-01 — Alignement matérialisé d'un risque d'une analyse vers son
 * statut avocat trichotomique et les outils décisionnels cibles (overlay
 * calculé au run de Synthèse enrichie par
 * {@link RisqueAlignmentService#materializeForAnalysis}).
 *
 * <p>Sérialisé en JSON dans {@code case_analyses.risques_alignment_json}.</p>
 *
 * <p>Pattern miroir {@link PieceManquanteAlignment} (F-194). Différence F-195 :
 * (a) le statut trichotomique est {@code A_CREUSER} / {@code VALIDE} /
 * {@code ECARTE} ; (b) chaque risque peut être mappé à un ou plusieurs outils
 * décisionnels via {@link RisqueToolMatcher} (keyword-based) ; (c) le score de
 * risque est recomputé en excluant les ÉCARTÉ — stocké séparément dans
 * {@code score_risque_avocat_json} pour préserver F-IA-02 strict.</p>
 *
 * @param risqueLibelle  libellé du risque tel qu'extrait par l'IA dans le JSON
 *                       {@code risques} (forme originale)
 * @param statut         statut avocat ({@code A_CREUSER} par défaut si overlay
 *                       absent / {@code VALIDE} / {@code ECARTE})
 * @param raisonEcarte   raison écarté (statut {@code ECARTE} uniquement) ou {@code null}
 * @param toolIdsCibles  liste d'identifiants TOOL_REGISTRY frontend pré-flag /
 *                       masquage selon statut (vide si {@code NO_TARGET_TOOL})
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RisqueAlignment(
        String risqueLibelle,
        String statut,
        String raisonEcarte,
        List<String> toolIdsCibles
) {
    public static final String STATUS_A_CREUSER = RisqueStatus.STATUT_A_CREUSER;
    public static final String STATUS_VALIDE = RisqueStatus.STATUT_VALIDE;
    public static final String STATUS_ECARTE = RisqueStatus.STATUT_ECARTE;
}
