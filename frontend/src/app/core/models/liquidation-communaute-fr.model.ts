/**
 * SF-216-06 : modèles TypeScript de l'outil "Liquidation communauté légale" (F-FA-04,
 * FRANCE uniquement — art. 1467-1517 Cciv + art. 1433-1435 Cciv + art. 815-832 Cciv).
 *
 * Contrat API importé de SF-216-05 (backend, endpoints POST/GET figés) :
 *   POST /api/v1/case-files/{caseFileId}/liquidation-communaute-fr
 *   GET  /api/v1/case-files/{caseFileId}/liquidation-communaute-fr
 *
 * Fondement :
 *  - art. 1467 Cciv : dissolution → indivision post-communautaire.
 *  - art. 1475 Cciv : partage égalitaire de la masse commune.
 *  - art. 1433-1435 Cciv : récompenses entre époux et communauté.
 *  - art. 815-832 Cciv : indivision résiduelle (logement non partagé).
 *  - art. 1364 / 1377 CPC : procédure notariale / judiciaire.
 *
 * Cet outil **remplace** le wrapper présentationnel SF-198-03 (qui restituait
 * `synthesis.liquidationCommunaute`) par un vrai simulateur backend persistant
 * (table `liquidation_communaute_fr_analyses`).
 */

/** Qui occupe le logement de la famille à la liquidation. */
export type OccupationLogement = 'EPOUX1' | 'EPOUX2' | 'AUCUN';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/liquidation-communaute-fr`.
 *
 * 11 champs (cf. backend `LiquidationCommunauteFrRequest`). Tous les `Integer`
 * Java sont des `number | null` côté frontend ; le backend rejette les valeurs
 * négatives en 400 mais accepte null (traité comme 0 par le calculateur).
 */
export interface LiquidationCommunauteFrRequest {
  valeurImmeubleCommun1Eur: number | null;
  valeurImmeubleCommun2Eur: number | null;
  capitalRestantDuEur: number | null;
  valeurMobilierCommunEur: number | null;
  autresActifsCommunsEur: number | null;
  recompensesEpoux1Eur: number | null;
  recompensesEpoux2Eur: number | null;
  biensPropresEpoux1Eur: number | null;
  biensPropresEpoux2Eur: number | null;
  occupationLogementEpoux: OccupationLogement | null;
  regimeMatrimonialDetecte: string | null;
}

/**
 * Réponse POST / GET. Le backend ne ré-expose PAS les inputs — seuls les
 * champs calculés sont retournés. La ré-édition du formulaire repart des
 * dernières valeurs saisies en mémoire frontend.
 */
export interface LiquidationCommunauteFrResponse {
  caseFileId: string;
  masseCommuneEur: number;
  quotaPartEpoux1Eur: number;
  quotaPartEpoux2Eur: number;
  soulteEur: number;
  recompensesNettesEpoux1Eur: number;
  recompensesNettesEpoux2Eur: number;
  alerteIndivision: boolean;
  baseJuridique: string;
  messages: string[];
  alertes: string[];
  country: string;
}
