/**
 * SF-212-30 : modèles TypeScript pour l'outil décisionnel "Congé maternité /
 * paternité — protection et indemnités" (F-DT-77-conge-paternite-maternite,
 * FR — L. 1225-1 à L. 1225-40 CT ; L. 331-3 CSS ; loi du 16/03/2021).
 *
 * Contrat API importé de SF-212-29 (backend, endpoints POST/GET figés sur
 * `/api/v1/case-files/{caseFileId}/conge-maternite-paternite`).
 */

/** Type de congé — MATERNITE ou PATERNITE. */
export type CongeMaternitePaterniteType = 'MATERNITE' | 'PATERNITE';

/** Code structuré F-IA-03 d'un point d'alerte. */
export type CongeMaternitePaterniteCodeCritere =
  | 'DT77_TYPE_CONGE'
  | 'DT77_DUREE_CONGE'
  | 'DT77_PROTECTION_LICENCIEMENT'
  | 'DT77_RETOUR_POSTE';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/conge-maternite-paternite`.
 *
 * 7 champs alignés sur SF-212-29 (figé) : type (select), rang enfant,
 * naissance multiple (toggle), salaire mensuel brut, date début (ISO),
 * licenciement pendant congé (toggle), retour poste différent (toggle).
 */
export interface CongeMaternitePaterniteRequest {
  typeConge: CongeMaternitePaterniteType;
  rangEnfant: number;
  naissanceMultiple: boolean;
  salaireMensuelBrutEuros: number;
  /** ISO YYYY-MM-DD. */
  dateDebutConge: string;
  licenciementPendantConge: boolean;
  retourPosteDifferent: boolean;
}

/**
 * Réponse de l'endpoint POST / GET — snapshot des inputs (ré-édition UI)
 * + sorties calculées (durée, IJ estimées, période de protection, alertes,
 * bases juridiques, messages).
 */
export interface CongeMaternitePaterniteResponse extends CongeMaternitePaterniteRequest {
  caseFileId: string;
  dureeCongeJours: number;
  /** ISO YYYY-MM-DD. */
  dateFinCongeTheorique: string;
  ijJournaliereEstimeeEuros: number;
  ijTotaleEstimeeEuros: number;
  /** ISO YYYY-MM-DD — date fin + 10 sem (L. 1225-4 CT). */
  protectionLicenciementJusqua: string;
  alerteLicenciementIllegal: boolean;
  alerteRetourPosteIllegal: boolean;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
