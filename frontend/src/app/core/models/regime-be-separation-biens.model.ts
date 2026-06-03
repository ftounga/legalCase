/**
 * SF-223-06 : modèles TypeScript de l'outil décisionnel "Régime de séparation
 * de biens (Belgique)" (`regime-be-separation-biens`).
 *
 * BELGIQUE uniquement. Contrat API figé côté backend SF-223-06 (endpoints
 * POST/GET). Régime de séparation de biens et ses variantes (Livre 3 CC ; loi
 * du 22/07/2018 — à vérifier par avocat belge). Cadre la SPÉCIFICITÉ BE
 * post-2018 (clause de participation aux acquêts + correctif équitable) —
 * DISTINCT du régime de COMMUNAUTÉ légale BE (`regime-mat-be-communaute-legale`,
 * F-217) et de la liquidation-partage notariale chiffrée
 * (`liquidation-partage-be`, F-217). BE-only pur.
 */

/** Variante du régime de séparation de biens. */
export type VarianteRegime =
  | 'SEPARATION_PURE'
  | 'SEPARATION_AVEC_SOCIETE_ACQUETS'
  | 'SEPARATION_AVEC_PARTICIPATION_ACQUETS';

/** Verdict de l'analyse (4 niveaux). */
export type RegimeBeSeparationBiensVerdict =
  | 'SEPARATION_PURE_QUALIFIEE'
  | 'SOCIETE_ACQUETS_QUALIFIEE'
  | 'PARTICIPATION_ACQUETS_QUALIFIEE'
  | 'QUALIFICATION_INCOMPLETE';

/**
 * Requête POST
 * `/api/v1/case-files/{caseFileId}/regime-be-separation-biens-analysis`.
 *
 * `varianteRegime` est requise (validation côté backend → 400) ;
 * `clauseParticipationPrevue` est requise pour la variante
 * SEPARATION_AVEC_PARTICIPATION_ACQUETS. Les autres champs sont nullables
 * (informatifs / pré-remplissables).
 */
export interface RegimeBeSeparationBiensRequest {
  varianteRegime: VarianteRegime;
  contratMariageNotarie: boolean | null;
  clauseParticipationPrevue: boolean | null;
  disproportionPatrimonialeAllegee: boolean | null;
  dateContrat: string | null;
  patrimoinePropreEpoux1Eur: number | null;
  patrimoinePropreEpoux2Eur: number | null;
}

/**
 * Réponse POST / GET. Ré-expose le snapshot des inputs (ré-édition du
 * formulaire) + les champs calculés (verdict, motifs, effets patrimoniaux,
 * bases juridiques, messages).
 */
export interface RegimeBeSeparationBiensResponse extends RegimeBeSeparationBiensRequest {
  caseFileId: string;
  verdict: RegimeBeSeparationBiensVerdict;
  motifs: string[];
  effetsPatrimoniaux: string;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
