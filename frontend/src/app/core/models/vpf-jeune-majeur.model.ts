/**
 * SF-220-03 : modèles miroirs du contrat API (backend SF-220-03) pour l'outil
 * décisionnel "VPF jeune majeur L.423-22"
 * (F-IM-49-vpf-jeune-majeur-l42322-fr). FR uniquement.
 *
 * Évalue l'éligibilité d'un jeune majeur (16-21 ans, entré mineur, scolarisé /
 * pris en charge par l'ASE) à la carte « vie privée et familiale » de
 * l'art. L.423-22 CESEDA (transition à la majorité / sortie ASE). Distinct de
 * F-IM-27 (VPF liens personnels L.423-23), F-IM-19 (mineurs) et F-IM-38
 * (évaluation de l'âge MNA).
 */

export type EligibiliteVpfJeuneMajeur =
  | 'ELIGIBLE_L42322'
  | 'ELIGIBLE_SOUS_RESERVE'
  | 'NON_ELIGIBLE'
  | 'ORIENTER_AES';

export interface VpfJeuneMajeurRequest {
  age: number;
  entreMineur: boolean;
  dateEntreeFrance: string | null;
  ageEntreeAse: number | null;
  priseEnChargeAse: boolean;
  dateDebutPriseEnCharge: string | null;
  ancienneteMoisPriseEnCharge: number | null;
  scolariseOuFormation: boolean;
  caractereReelEtSerieuxFormation: boolean;
  avisStructureFavorable: boolean;
  absenceLienFamillePays: boolean;
}

export interface VpfJeuneMajeurResponse {
  caseFileId: string;
  age: number;
  entreMineur: boolean;
  dateEntreeFrance: string | null;
  ageEntreeAse: number | null;
  priseEnChargeAse: boolean;
  dateDebutPriseEnCharge: string | null;
  ancienneteMoisPriseEnCharge: number | null;
  scolariseOuFormation: boolean;
  caractereReelEtSerieuxFormation: boolean;
  country: string;
  eligibilite: EligibiliteVpfJeuneMajeur;
  ancienneteRequiseMois: number;
  criteresManquants: string[];
  basesJuridiques: string[];
  messages: string[];
}
