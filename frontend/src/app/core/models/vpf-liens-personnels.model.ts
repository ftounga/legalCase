/**
 * SF-214-06 : modèles miroirs du contrat API (backend SF-214-05) pour l'outil
 * décisionnel « Vie privée et familiale — liens personnels L.423-23 CESEDA »
 * (F-IM-27-vpf-liens-personnels-l42323-fr).
 * FRANCE uniquement (CESEDA L.423-23 — scoring liens personnels et familiaux).
 */

export type NiveauIntegration = 'FORT' | 'MOYEN' | 'FAIBLE';

export type VerdictVpfLiensPersonnels =
  | 'ELIGIBLE_PROBABLE'
  | 'ELIGIBLE_SOUS_RESERVE'
  | 'NON_ELIGIBLE'
  | 'DOSSIER_A_CONSOLIDER';

export interface VpfLiensPersonnelsRequest {
  dureeResidenceFranceMois: number;
  entreeEnFranceMineur: boolean;
  enfantsEnFrance: boolean;
  conjointEnFrance: boolean;
  parentsEnFrance: boolean;
  situationFamilialeAlEtranger?: string | null;
  niveauIntegration: NiveauIntegration;
  ancienneConvictionPenale: boolean;
}

export interface VpfLiensPersonnelsResponse {
  caseFileId: string;
  dureeResidenceFranceMois: number;
  entreeEnFranceMineur: boolean;
  enfantsEnFrance: boolean;
  conjointEnFrance: boolean;
  parentsEnFrance: boolean;
  situationFamilialeAlEtranger?: string | null;
  niveauIntegration: NiveauIntegration;
  ancienneConvictionPenale: boolean;
  country: string;
  verdict: VerdictVpfLiensPersonnels;
  score: number;
  chipsCriteresNonRemplis: string[];
  recommandations: string[];
  baseJuridique?: string;
}
