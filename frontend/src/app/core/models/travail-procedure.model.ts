/**
 * SF-136-02 : modèle TS du calendrier procédural travail (FR + BE).
 * Strictement aligné sur les 6 codes seedés en SF-136-01 dans la table
 * `legal_referentials` (type `TRAVAIL_PROCEDURE_JALONS`) et sur le DTO backend
 * `TravailProcedureJalonResponse`.
 */

/** Codes procédure FR. */
export type TravailProcedureCodeFr =
  | 'PRUDHOMMES_FR'
  | 'APPEL_CA_SOCIALE_FR'
  | 'CASSATION_SOCIALE_FR';

/** Codes procédure BE. */
export type TravailProcedureCodeBe =
  | 'TRIBUNAL_TRAVAIL_BE'
  | 'COUR_TRAVAIL_BE'
  | 'CASSATION_BE';

/** Union des 6 codes. */
export type TravailProcedureCode = TravailProcedureCodeFr | TravailProcedureCodeBe;

export interface TravailProcedureOption {
  code: TravailProcedureCode;
  label: string;
}

/** Liste des options FR (3) — ordre instance / appel / cassation. */
export const TRAVAIL_PROCEDURE_OPTIONS_FR: ReadonlyArray<TravailProcedureOption> = [
  { code: 'PRUDHOMMES_FR', label: "Conseil de prud'hommes (BCO + bureau de jugement)" },
  { code: 'APPEL_CA_SOCIALE_FR', label: "Appel — chambre sociale Cour d'appel" },
  { code: 'CASSATION_SOCIALE_FR', label: "Pourvoi en cassation — chambre sociale" },
];

/** Liste des options BE (3) — ordre instance / appel / cassation. */
export const TRAVAIL_PROCEDURE_OPTIONS_BE: ReadonlyArray<TravailProcedureOption> = [
  { code: 'TRIBUNAL_TRAVAIL_BE', label: "Tribunal du travail (1ʳᵉ instance)" },
  { code: 'COUR_TRAVAIL_BE', label: "Cour du travail (appel)" },
  { code: 'CASSATION_BE', label: "Pourvoi en cassation — Cour de cassation" },
];

/** Tous les codes valides (utilisé pour valider un pré-fill IA). */
export const TRAVAIL_PROCEDURE_CODES: ReadonlySet<TravailProcedureCode> = new Set<TravailProcedureCode>([
  'PRUDHOMMES_FR', 'APPEL_CA_SOCIALE_FR', 'CASSATION_SOCIALE_FR',
  'TRIBUNAL_TRAVAIL_BE', 'COUR_TRAVAIL_BE', 'CASSATION_BE',
]);

/**
 * SF-136-02 : DTO d'un jalon retourné par
 * `GET /api/v1/case-files/{caseFileId}/travail-procedure-jalons`.
 */
export interface TravailProcedureJalon {
  code: TravailProcedureCode;
  label: string;
  offsetDays: number;
  articleRef: string;
  /** ISO `YYYY-MM-DD` si `dateDeclencheur` fourni au backend ; sinon `null`. */
  dateCible: string | null;
}
