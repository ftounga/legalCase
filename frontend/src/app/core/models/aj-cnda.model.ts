/**
 * SF-214-20 : modèles miroirs du contrat API (backend SF-214-19) pour l'outil
 * décisionnel "Aide juridictionnelle CNDA" (F-IM-34-aj-cnda-fr).
 * FRANCE UNIQUEMENT — l'aide juridictionnelle devant la Cour nationale du droit
 * d'asile (CNDA) est spécifique au droit français.
 *
 * Le backend vérifie l'éligibilité ressources, calcule l'échéance du recours
 * CNDA (1 mois de la notification OFPRA, réduit en procédure accélérée) et
 * l'échéance de dépôt de la demande d'AJ, et fournit la liste des pièces.
 */

export type StatutAjCnda =
  | 'AJ_A_DEMANDER'
  | 'AJ_DEPOSEE'
  | 'HORS_DELAI_AJ'
  | 'NON_ELIGIBLE_RESSOURCES';

export interface AjCndaRequest {
  dateDecisionOFPRA: string; // YYYY-MM-DD, requis
  ressourcesMensuellesNettes: number;
  procedureAcceleree: boolean;
  demandeAJDeposee: boolean;
  dateDepotAJ?: string | null; // YYYY-MM-DD, optionnel
}

export interface AjCndaResponse {
  caseFileId: string;
  dateDecisionOFPRA: string;
  ressourcesMensuellesNettes: number;
  procedureAcceleree: boolean;
  demandeAJDeposee: boolean;
  dateDepotAJ?: string | null;
  country: string;
  statut: StatutAjCnda;
  eligibleAJ: boolean;
  dateEcheanceRecoursCNDA: string; // YYYY-MM-DD
  dateEcheanceDemandeAJ: string; // YYYY-MM-DD
  procedureAccelereeDureeReduite: boolean;
  piecesAJ: string[];
}
