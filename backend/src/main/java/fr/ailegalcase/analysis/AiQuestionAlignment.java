package fr.ailegalcase.analysis;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

/**
 * F-196 SF-196-01 — Alignement matérialisé d'une question complémentaire F-94
 * (avec ou sans réponse avocat) vers la pièce déduite (statut PIECE_OBTENUE /
 * PIECE_MANQUANTE / INFO_ONLY) — calculé au run de Synthèse enrichie par
 * {@link AiQuestionAlignmentService#materializeForAnalysis}.
 *
 * <p>Sérialisé en JSON dans {@code case_analyses.ai_questions_alignment_json}.</p>
 *
 * <p>Pattern miroir {@link PieceManquanteAlignment} (F-194) /
 * {@link RisqueAlignment} (F-195). Différence F-196 : la matérialisation
 * exploite un mapping statique {@code questionText → pieceLibelle} via
 * {@link AiQuestionPieceExtractor} et le statut est déduit de la réponse :
 * <ul>
 *   <li>réponse ~"oui" + pièce extraite → {@code PIECE_OBTENUE}</li>
 *   <li>réponse ~"non" + pièce extraite → {@code PIECE_MANQUANTE}</li>
 *   <li>pas de réponse, pas de pièce extractible, ou réponse hors
 *       oui/non → {@code INFO_ONLY}</li>
 * </ul>
 *
 * <p>Cohérence F-94 STRICTE : la PUT réponse F-94 ne crée PAS de pièce ; les
 * pièces auto sont propagées exclusivement au prochain run de Synthèse
 * enrichie via {@code AiQuestionAlignmentService.propagateToPiecesManquantes}
 * (analogue F-192/F-193).</p>
 *
 * @param questionId          identifiant {@link AiQuestion}
 * @param answerText          réponse libre de l'avocat (peut être {@code null}
 *                            si la question n'a pas encore été répondue)
 * @param pieceLibelleDeduit  libellé de pièce déduit (V1 : keyword statique,
 *                            cf. {@link AiQuestionPieceExtractor}), ou
 *                            {@code null} si rien d'extractible
 * @param statutDeduction     {@code PIECE_OBTENUE} / {@code PIECE_MANQUANTE} /
 *                            {@code INFO_ONLY}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiQuestionAlignment(
        UUID questionId,
        String answerText,
        String pieceLibelleDeduit,
        String statutDeduction
) {
    public static final String STATUT_PIECE_OBTENUE = "PIECE_OBTENUE";
    public static final String STATUT_PIECE_MANQUANTE = "PIECE_MANQUANTE";
    public static final String STATUT_INFO_ONLY = "INFO_ONLY";
}
