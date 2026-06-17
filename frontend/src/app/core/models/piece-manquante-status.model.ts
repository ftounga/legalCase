/**
 * F-194 SF-194-02 — Statut décidé par l'avocat sur une pièce manquante listée
 * par la synthèse IA. Trichotomie cohérente avec F-176 (strategic-option) :
 *
 * <ul>
 *   <li><b>A_DEMANDER</b>     : pièce à réclamer (au client, à la partie
 *       adverse, à une administration). Statut implicite par défaut tant que
 *       l'avocat n'a pas encore tagué la pièce.</li>
 *   <li><b>OBTENUE</b>        : pièce reçue. La pièce sera ensuite
 *       matérialisée vers les outils décisionnels au prochain run de Synthèse
 *       enrichie pour pré-cocher les checkbox correspondantes.</li>
 *   <li><b>NON_APPLICABLE</b> : pièce volontairement écartée par l'avocat
 *       (raison libre optionnelle). Cohérent F-176 statut DISCARDED gris.</li>
 * </ul>
 *
 * <p>Cohérence F-176 stricte : le PUT statut est un acte pur côté backend (pas
 * de side-effect, pas de recompute). La matérialisation pièce → outil ne se
 * fait qu'au prochain run de Synthèse enrichie via l'event SSE
 * {@code ENRICHED_ANALYSIS DONE}.</p>
 */
export type PieceManquanteStatutValue = 'A_DEMANDER' | 'OBTENUE' | 'NON_APPLICABLE';

/**
 * Destinataires types proposés à l'avocat lorsqu'il marque une pièce comme
 * `A_DEMANDER`. La valeur est libre côté backend (texte) — la liste sert
 * uniquement à proposer des choix usuels dans le `<mat-select>`.
 */
export const PIECE_DESTINATAIRES = [
  'Client',
  'Ex-employeur',
  'Préfecture',
  'Tribunal',
  'Avocat adverse',
  'Autre',
] as const;

export type PieceDestinataire = typeof PIECE_DESTINATAIRES[number];

/**
 * Body envoyé au PUT /api/v1/case-files/{id}/pieces-manquantes/status.
 * <p>{@code pieceLibelleOriginal} = libellé exact tel qu'affiché par la
 * synthèse (issu de l'IA). Sert de clé d'upsert côté backend.</p>
 */
export interface PieceManquanteStatusPayload {
  pieceLibelleOriginal: string;
  statut: PieceManquanteStatutValue;
  raisonNonApp?: string | null;
  destinataire?: string | null;
  /** SF-194-04 — document reçu lié à la pièce obtenue (optionnel). */
  documentId?: string | null;
}

/**
 * Réponse 200 du PUT, miroir du DTO backend `PieceManquanteStatusResponse`.
 * Contient l'entrée upsertée (pour rafraîchir le signal local côté UI).
 */
export interface PieceManquanteStatus {
  pieceLibelleOriginal: string;
  statut: PieceManquanteStatutValue;
  raisonNonApp?: string | null;
  destinataire?: string | null;
  updatedAt?: string | null;
}
