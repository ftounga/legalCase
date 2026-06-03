/**
 * SF-222-01 : modèles TypeScript de l'outil "Allocation de soutien familial"
 * (F-FA-ASF-CAF, FRANCE uniquement — art. L. 523-1 CSS).
 *
 * Contrat API figé côté backend SF-222-01 (POST/GET) :
 *   POST /api/v1/case-files/{caseFileId}/asf-caf-analysis
 *   GET  /api/v1/case-files/{caseFileId}/asf-caf-analysis
 *
 * Anti-doublon : distinct de F-FA-ARIPA-RECOUVREMENT (recouvrement forcé de la
 * pension). L'ASF = droit + montant de la prestation CAF ; cet outil renvoie
 * vers ARIPA pour le recouvrement.
 */

/** État de paiement de la pension par l'autre parent. */
export type PensionPayee = 'NON_PAYEE' | 'PARTIELLE' | 'PAYEE';

/** Verdict 4 niveaux du calculateur ASF. */
export type VerdictAsf =
  | 'DROIT_ASF_PLEIN'
  | 'DROIT_ASF_DIFFERENTIELLE'
  | 'DROIT_AVEC_RECOUVREMENT'
  | 'PAS_DE_DROIT';

/** Requête POST `/api/v1/case-files/{caseFileId}/asf-caf-analysis` (6 champs). */
export interface AsfCafRequest {
  parentIsole: boolean | null;
  pensionFixee: boolean | null;
  montantPensionMensuel: number | null;
  pensionPayee: PensionPayee | null;
  nbEnfantsACharge: number | null;
  autreParentDecedeOuInconnu: boolean | null;
}

/** Réponse POST / GET — résultat persisté pour le dossier. */
export interface AsfCafResponse {
  caseFileId: string;
  verdict: VerdictAsf;
  montantMensuelEstime: number;
  recouvrementApplicable: boolean;
  basesJuridiques: string[];
  messages: string[];
  country: string;
}
