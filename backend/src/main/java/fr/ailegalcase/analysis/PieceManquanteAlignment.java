package fr.ailegalcase.analysis;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * F-194 SF-194-01 — Alignement matérialisé d'une pièce manquante d'une analyse
 * vers son statut avocat trichotomique (overlay calculé au run de Synthèse
 * enrichie par {@link PieceManquanteAlignmentService#materializeForAnalysis}).
 *
 * <p>Sérialisé en JSON dans {@code case_analyses.pieces_alignment_json}.</p>
 *
 * <p>Pattern miroir {@link RetainedPisteAlignment} (F-192) et
 * {@link ProcedureCheckAlignment} (F-193). Différence F-194 : le statut est
 * directement la valeur posée par l'avocat (pas un {@code matchStatus} calculé
 * vs une cible outil) — la matérialisation joint le tableau JSON
 * {@code pieces_manquantes} (régénéré à chaque run par l'IA) et l'overlay
 * {@code piece_manquante_status} via libellé normalisé.</p>
 *
 * @param pieceLibelle libellé de la pièce tel qu'extrait par l'IA dans le JSON
 *                     {@code pieces_manquantes} (forme originale)
 * @param statut       statut avocat ({@code A_DEMANDER} par défaut si overlay
 *                     absent / {@code OBTENUE} / {@code NON_APPLICABLE})
 * @param destinataire destinataire ({@code "client"}, {@code "ex-employeur"}, etc.) ou {@code null}
 * @param raisonNonApp raison non applicable (statut {@code NON_APPLICABLE} uniquement) ou {@code null}
 * @param obtainedDocumentId SF-194-04 — document du dossier lié à la pièce reçue
 *                     (statut {@code OBTENUE}) pour la traçabilité, ou {@code null}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PieceManquanteAlignment(
        String pieceLibelle,
        String statut,
        String destinataire,
        String raisonNonApp,
        java.util.UUID obtainedDocumentId
) {
    public static final String STATUS_A_DEMANDER = PieceManquanteStatus.STATUT_A_DEMANDER;
    public static final String STATUS_OBTENUE = PieceManquanteStatus.STATUT_OBTENUE;
    public static final String STATUS_NON_APPLICABLE = PieceManquanteStatus.STATUT_NON_APPLICABLE;
}
