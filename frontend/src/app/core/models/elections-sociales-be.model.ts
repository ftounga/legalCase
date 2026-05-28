/**
 * SF-219-09b : modèle frontend de l'outil F-219 — Élections sociales
 * (Belgique) — Loi du 04/12/2007 + AR du 25/05/2012 + Loi 04/08/1996
 * art. 49 + Loi 19/03/1991 (protection des candidats).
 *
 * <p><b>BE uniquement</b> — les élections CSE françaises (Code du
 * travail art. L. 2311-1 et s.) ont un calendrier libre déclenché
 * par l'employeur à tout moment dans le cycle 4 ans, des seuils
 * différents (11 / 50 / 300), pas de jour Y national imposé.
 * Aucun mapping possible.</p>
 *
 * <p>Contrat figé dans SF-219-09 backend
 * ({@code ElectionsSocialesBeRequest} + {@code ElectionsSocialesBeResponse}).</p>
 */

/**
 * Cycle électoral 4 ans imposé par AR pour les élections sociales BE.
 *
 * <ul>
 *   <li>{@code CYCLE_2024} — fenêtre Y : 13 au 26 mai 2024
 *       (AR 14/07/2022).</li>
 *   <li>{@code CYCLE_2028} — fenêtre Y annoncée : 11 au 24 mai 2028.</li>
 *   <li>{@code CYCLE_2032} — fenêtre Y prévisionnelle : mai 2032.</li>
 * </ul>
 */
export type ElectionsSocialesBeCycle =
  | 'CYCLE_2024'
  | 'CYCLE_2028'
  | 'CYCLE_2032';

/**
 * Verdict 4 états de l'outil élections sociales BE.
 *
 * <ul>
 *   <li>{@code OBLIGATION_CE_ET_CPPT} — ≥ 100 ETP : CE + CPPT
 *       obligatoires (vert).</li>
 *   <li>{@code OBLIGATION_CPPT_SEUL} — 50-99 ETP : CPPT seul obligatoire
 *       (ambre / vert selon présentation).</li>
 *   <li>{@code NON_APPLICABLE_SEUIL} — &lt; 50 ETP : pas d'obligation
 *       (gris info).</li>
 *   <li>{@code EFFECTIF_A_RECALCULER} — effectif limite (49-51 ou
 *       99-101 ETP) — recalcul ETP requis (ambre).</li>
 * </ul>
 */
export type ElectionsSocialesBeVerdict =
  | 'OBLIGATION_CE_ET_CPPT'
  | 'OBLIGATION_CPPT_SEUL'
  | 'NON_APPLICABLE_SEUIL'
  | 'EFFECTIF_A_RECALCULER';

/**
 * Body POST {@code /api/v1/case-files/{caseFileId}/decision-tools/elections-sociales-be}.
 *
 * <p>Tous les champs sont requis côté backend ({@code @NotNull}).</p>
 */
export interface ElectionsSocialesBeRequest {
  /** Cycle électoral 4 ans (le jour Y doit être dans la fenêtre nationale). */
  cycleElectoral: ElectionsSocialesBeCycle;
  /** Date du scrutin (jour Y) choisie par l'employeur dans la fenêtre AR. */
  dateJourY: string;
  /** Effectif moyen ETP calculé sur les 4 trimestres précédents. */
  effectifMoyenEtp: number;
  /** L'UTE retenue est-elle déjà confirmée (sinon procédure UTE à initier X-60). */
  uniteTechniqueExploitationConfirmee: boolean;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + verdict +
 * calendrier complet calculé à partir de Y + fenêtre protection candidats.
 */
export interface ElectionsSocialesBeResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  cycleElectoral: ElectionsSocialesBeCycle;
  dateJourY: string;
  effectifMoyenEtp: number;
  uniteTechniqueExploitationConfirmee: boolean;

  // -- Verdict --
  verdict: ElectionsSocialesBeVerdict;
  /** CE obligatoire (seuil ≥ 100 ETP). */
  conseilEntrepriseObligatoire: boolean;
  /** CPPT obligatoire (seuil ≥ 50 ETP). */
  comitePreventionProtectionObligatoire: boolean;
  /** Seuil CE appliqué (100). */
  seuilCeApplique: number;
  /** Seuil CPPT appliqué (50). */
  seuilCpptApplique: number;

  // -- Calendrier rebours (calculé à partir de Y) --
  /** Y - 90 : affichage de l'avis des élections. */
  jourX: string;
  /** X - 60 : début de la procédure UTE. */
  dateDebutProcedureUte: string;
  /** X - 35 : transmission des listes électorales. */
  dateTransmissionListesElecteurs: string;
  /** X + 35 : dépôt des listes de candidats. */
  dateDepotListesCandidats: string;
  /** X + 40 : affichage des candidats — déclenche la période protégée. */
  dateAffichageCandidats: string;
  /** Y + 6 : proclamation des résultats. */
  dateProclamationResultats: string;
  /** Y + 45 : première réunion CE / CPPT installé. */
  datePremiereReunionInstance: string;

  // -- Fenêtre de protection candidats (Loi 19/03/1991) --
  /** Début période protégée (≈ 30 j avant affichage candidats) ou null. */
  dateDebutPeriodeProtegeeCandidats: string | null;
  /** Fin période protégée non-élus (affichage + 2 ans) ou null. */
  dateFinPeriodeProtegeeNonElus: string | null;

  // -- Synthèse + métadonnées légales --
  /** Code raison du verdict ou null. */
  raison: string | null;
  /** Synthèse humaine 1-2 phrases. */
  synthese: string;
  /** Base juridique courte. */
  baseJuridique: string;
  /** Avertissement éventuel. */
  avertissement: string;
}
