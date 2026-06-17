import { PieceManquanteStatutValue } from './piece-manquante-status.model';

/**
 * F-194 SF-194-02 — Alignement matérialisé entre une pièce manquante taggée par
 * l'avocat et les outils décisionnels qui en dépendent.
 *
 * Miroir du DTO `PieceManquanteAlignmentResponse` exposé par le backend
 * (cf. SF-194-01). Pattern jumeau de {@link RetainedPisteAlignment} (F-192) et
 * {@link ProcedureCheckAlignment} (F-193) : la matérialisation est figée au
 * dernier run de Synthèse enrichie ; le PUT statut pièce ne déclenche AUCUN
 * refresh côté frontend (cohérence F-176 stricte).
 *
 * <p>Champs :
 * <ul>
 *   <li>{@code pieceLibelle}     : libellé exact de la pièce, tel que produit
 *       par l'IA dans la synthèse.</li>
 *   <li>{@code statut}           : statut décidé par l'avocat
 *       (A_DEMANDER / OBTENUE / NON_APPLICABLE).</li>
 *   <li>{@code toolIdsCibles}    : identifiants des outils décisionnels qui
 *       dépendent de cette pièce. Vide si aucun outil mappé (pièce
 *       transversale — comptée dans la tile dashboard, exclue des sorties
 *       outils).</li>
 *   <li>{@code destinataire}     : libre, renseigné par l'avocat si statut
 *       = A_DEMANDER.</li>
 *   <li>{@code raisonNonApp}     : libre, renseigné par l'avocat si statut
 *       = NON_APPLICABLE.</li>
 * </ul></p>
 */
export interface PieceManquanteAlignment {
  pieceLibelle: string;
  statut: PieceManquanteStatutValue;
  toolIdsCibles: string[];
  destinataire?: string | null;
  raisonNonApp?: string | null;
  /** SF-194-04 — document du dossier lié à la pièce reçue (null si non lié). */
  obtainedDocumentId?: string | null;
}
