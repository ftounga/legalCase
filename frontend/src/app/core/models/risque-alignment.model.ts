import { RisqueStatutValue } from './risque-status.model';

/**
 * F-195 SF-195-02 — Alignement matérialisé entre un risque taggé par l'avocat
 * dans la synthèse et les outils décisionnels qui en dépendent.
 *
 * Miroir du DTO `RisqueAlignmentResponse` exposé par le backend (cf. SF-195-01).
 * Pattern jumeau de {@link RetainedPisteAlignment} (F-192),
 * {@link ProcedureCheckAlignment} (F-193) et {@link PieceManquanteAlignment}
 * (F-194) : la matérialisation est figée au dernier run de Synthèse enrichie ;
 * le PUT statut risque ne déclenche AUCUN refresh côté frontend (cohérence
 * F-176 stricte).
 *
 * <p>Champs :
 * <ul>
 *   <li>{@code risqueLibelle}      : libellé exact du risque, tel que produit
 *       par l'IA dans la synthèse.</li>
 *   <li>{@code statut}             : statut décidé par l'avocat
 *       (A_CREUSER / VALIDE / ECARTE).</li>
 *   <li>{@code toolIdsCibles}      : identifiants des outils décisionnels qui
 *       dépendent de ce risque (mapping keyword-based — harcèlement →
 *       F-DT-12, etc.). Vide si aucun outil mappé.</li>
 *   <li>{@code raisonEcarte}       : libre, renseigné par l'avocat si statut
 *       = ECARTE.</li>
 * </ul></p>
 *
 * <p><strong>Contrat figé en avance</strong> : ce fichier précède le merge de
 * SF-195-02 (frontend) et SF-195-01 (backend), pattern utilisé pour SF-193-03
 * et SF-194-03. Réconciliation au merge des PRs parallèles.</p>
 */
export interface RisqueAlignment {
  risqueLibelle: string;
  statut: RisqueStatutValue;
  toolIdsCibles?: string[];
  raisonEcarte?: string | null;
}
