/**
 * SF-214-18 : modèles miroirs du contrat API (backend SF-214-17) pour l'outil
 * décisionnel « OFPRA introduction » (F-IM-33). FR uniquement.
 *
 * Calcule la date d'échéance d'introduction de la demande d'asile auprès de
 * l'OFPRA à partir de la date d'arrivée en France et du passage au guichet
 * unique (GUDA), liste les étapes ordonnées de la procédure (stepper 5 étapes)
 * et les pièces requises, et alerte sur le risque de procédure accélérée.
 */

export type StatutDelaiOfpra = 'A_DEPOSER' | 'URGENT' | 'EXPIRE';

export interface OfpraIntroductionRequest {
  dateArriveeEnFrance: string; // ISO yyyy-MM-dd (requis)
  passageGudaEffectue: boolean;
  datePassageGuda?: string | null; // ISO yyyy-MM-dd (optionnel)
  adaRequise: boolean;
  paysOrigine?: string | null; // optionnel
}

export interface OfpraIntroductionResponse {
  caseFileId: string;
  country: string;
  dateArriveeEnFrance: string;
  passageGudaEffectue: boolean;
  datePassageGuda?: string | null;
  adaRequise: boolean;
  paysOrigine?: string | null;
  dateEcheanceIntroduction: string; // ISO yyyy-MM-dd
  statutDelai: StatutDelaiOfpra;
  etapesAPrendre: string[];
  piecesRequises: string[];
  procedureAccelereeRisque: boolean;
}
