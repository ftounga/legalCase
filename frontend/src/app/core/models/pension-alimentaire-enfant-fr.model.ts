/**
 * SF-216-04 : modèles TypeScript de l'outil "Pension alimentaire enfant FR"
 * (F-FA-02, FRANCE uniquement — art. 371-2 Cciv + barème indicatif Cour de
 * cassation 2010 révisé + art. 373-2-2 Cciv révision).
 *
 * Contrat API importé de SF-216-03 (backend, endpoints POST/GET figés) :
 *   POST /api/v1/case-files/{caseFileId}/pension-alimentaire-enfant-fr
 *   GET  /api/v1/case-files/{caseFileId}/pension-alimentaire-enfant-fr
 *
 * Cet outil **remplace** le wrapper présentationnel SF-198-02 (qui restituait
 * `synthesis.pensionAlimentaireEstimate`) par un vrai simulateur backend
 * persistant (table `pension_alimentaire_enfant_fr_analyses`).
 */

/** Mode de résidence des enfants (impact coefficient barème). */
export type ModeResidenceEnfant = 'ALTERNEE' | 'PRINCIPALE_PARENT1' | 'PRINCIPALE_PARENT2';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/pension-alimentaire-enfant-fr`.
 *
 * 5 champs (cf. backend `PensionAlimentaireEnfantFrRequest`). Les revenus sont
 * en NETS MENSUELS (€ entiers). Le backend rejette les valeurs négatives en 400.
 *
 * `agesEnfants` : longueur = `nombreEnfants` ou 400.
 */
export interface PensionAlimentaireEnfantFrRequest {
  revenusNetsParent1Eur: number | null;
  revenusNetsParent2Eur: number | null;
  nombreEnfants: number | null;
  agesEnfants: number[];
  modeResidence: ModeResidenceEnfant | null;
}

/**
 * Réponse POST / GET. Le backend ne ré-expose PAS les inputs — seuls les
 * champs calculés sont retournés. La ré-édition du formulaire repart des
 * dernières valeurs saisies en mémoire frontend.
 */
export interface PensionAlimentaireEnfantFrResponse {
  caseFileId: string;
  montantParEnfantMensuelEur: number[];
  totalMensuelEur: number;
  tauxApplique: number;
  coefficientResidence: number;
  parentDebiteur: 'PARENT1' | 'PARENT2' | 'AUCUN' | string;
  baseJuridique: string;
  messages: string[];
  alertes: string[];
  country: string;
}
