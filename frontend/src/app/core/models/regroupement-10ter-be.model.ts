/**
 * SF-215-03 / SF-215-04 — modèles miroirs du contrat API (backend) pour l'outil
 * décisionnel « Regroupement familial art. 10ter (BE) » (F-IM-26-regroupement-10ter-be).
 *
 * BELGIQUE uniquement — scoring d'éligibilité pour le regroupement familial
 * sous l'art. 10ter de la Loi du 15/12/1980 sur l'accès au territoire, le
 * séjour, l'établissement et l'éloignement des étrangers, et l'AR du 17/05/2007
 * fixant le seuil de ressources à 120 % du Revenu d'intégration sociale (RIS)
 * — seuil tarifaire opérationnel : 1 500 €/mois (référence outil, valeur indicative).
 */

export type Regroupement10terLienFamilial =
  | 'CONJOINT'
  | 'PARTENAIRE_ENREGISTRE'
  | 'ENFANT_MOINS_21'
  | 'ENFANT_21_PLUS_CHARGE'
  | 'ASCENDANT_CHARGE';

export type Regroupement10terTypeCarte = 'CARTE_B' | 'CARTE_C';

export type Regroupement10terVerdict = 'ELIGIBLE' | 'SOUS_RESERVE' | 'INELIGIBLE';

export interface Regroupement10terBeRequest {
  lienFamilial: Regroupement10terLienFamilial;
  typeCarteRegroupant: Regroupement10terTypeCarte;
  revenusMensuelsNetsRegroupant: number; // entier 0-100 000
  dureeSejour: number;                    // entier mois 0-600
  logementConforme: boolean;
  assuranceMaladie: boolean;
  menaceOrdrePublic: boolean;
}

export interface Regroupement10terBeResponse {
  caseFileId: string;
  lienFamilial: string;
  typeCarteRegroupant: string;
  revenusMensuelsNetsRegroupant: number;
  dureeSejour: number;
  logementConforme: boolean;
  assuranceMaladie: boolean;
  menaceOrdrePublic: boolean;
  seuilRessources: number;        // 1500
  differentielRevenus: number;    // signé : revenus - seuil
  scoreEligibilite: number;       // 0-100
  verdict: Regroupement10terVerdict;
  criteresNonRemplis: string[];
  baseJuridique: string;
}

export interface Regroupement10terLienFamilialOption {
  code: Regroupement10terLienFamilial;
  label: string;
}

export interface Regroupement10terTypeCarteOption {
  code: Regroupement10terTypeCarte;
  label: string;
}

export const REGROUPEMENT_10TER_LIENS_FAMILIAUX: ReadonlyArray<Regroupement10terLienFamilialOption> = [
  { code: 'CONJOINT', label: 'Conjoint' },
  { code: 'PARTENAIRE_ENREGISTRE', label: 'Partenaire enregistré' },
  { code: 'ENFANT_MOINS_21', label: 'Enfant de moins de 21 ans' },
  { code: 'ENFANT_21_PLUS_CHARGE', label: 'Enfant majeur à charge' },
  { code: 'ASCENDANT_CHARGE', label: 'Ascendant à charge' },
];

export const REGROUPEMENT_10TER_TYPES_CARTE: ReadonlyArray<Regroupement10terTypeCarteOption> = [
  { code: 'CARTE_B', label: 'Carte B (séjour limité)' },
  { code: 'CARTE_C', label: 'Carte C (séjour illimité)' },
];

/** Seuil opérationnel de ressources mensuelles nettes (AR 17/05/2007). */
export const SEUIL_RESSOURCES_DEFAULT = 1_500;
