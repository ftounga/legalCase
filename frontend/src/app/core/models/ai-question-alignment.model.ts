/**
 * F-196 SF-196-02 — Statut de déduction d'une question complémentaire IA
 * répondue par l'avocat (F-94), tel que matérialisé par le backend au dernier
 * run de Synthèse enrichie.
 *
 * <ul>
 *   <li><b>PIECE_OBTENUE</b>  : la réponse de l'avocat indique que la pièce
 *       déduite (ex. « Quel est le motif du licenciement ? ») a été obtenue —
 *       le backend matérialisera la pièce vers les outils au prochain run.</li>
 *   <li><b>PIECE_MANQUANTE</b> : la réponse indique l'absence ou
 *       l'indisponibilité d'une pièce.</li>
 *   <li><b>INFO_ONLY</b>      : la réponse a une valeur informationnelle pure
 *       (ex. clarification d'un délai déjà connu) — pas de matérialisation
 *       pièce.</li>
 * </ul>
 */
export type AiQuestionStatutDeduction = 'PIECE_OBTENUE' | 'PIECE_MANQUANTE' | 'INFO_ONLY';

/**
 * F-196 SF-196-02 — Alignement matérialisé entre une question complémentaire
 * répondue par l'avocat (F-94) et les outils décisionnels qui peuvent en
 * bénéficier.
 *
 * Miroir du DTO `AiQuestionAlignmentResponse` exposé par le backend
 * (cf. SF-196-01). Pattern jumeau de {@link RetainedPisteAlignment} (F-192),
 * {@link ProcedureCheckAlignment} (F-193), {@link PieceManquanteAlignment}
 * (F-194) et {@link RisqueAlignment} (F-195) : la matérialisation est figée
 * au dernier run de Synthèse enrichie ; le PUT réponse F-94 ne déclenche
 * AUCUN refresh côté frontend (cohérence F-176 stricte).
 *
 * <p>Champs :
 * <ul>
 *   <li>{@code questionId}          : identifiant de la question (UUID).</li>
 *   <li>{@code answerText}          : texte libre de la réponse de l'avocat
 *       (vide si pas encore répondu).</li>
 *   <li>{@code pieceLibelleDeduit}  : libellé de la pièce déduite par
 *       l'analyse de la réponse, présent si {@code statutDeduction} ∈
 *       {PIECE_OBTENUE, PIECE_MANQUANTE}.</li>
 *   <li>{@code statutDeduction}     : statut de déduction (cf. type ci-dessus).</li>
 * </ul></p>
 *
 * <p><strong>Contrat figé en avance</strong> : ce fichier précède le merge de
 * SF-196-02 (frontend) et SF-196-01 (backend), pattern utilisé pour
 * SF-194-02 et SF-195-02. Réconciliation au merge des PRs parallèles.</p>
 */
export interface AiQuestionAlignment {
  questionId: string;
  /**
   * Libellé exact de la question telle que produite par l'IA (optionnel).
   * Utilisé par l'export PDF SF-196-03 pour afficher la Q&R.
   */
  questionText?: string;
  answerText?: string | null;
  pieceLibelleDeduit?: string | null;
  /**
   * Optionnel — INFO_ONLY si la réponse n'a pas de pièce déduite, undefined
   * autorisé pour les questions non répondues.
   */
  statutDeduction?: AiQuestionStatutDeduction;
}
