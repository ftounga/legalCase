/**
 * SF-215-05 / SF-215-06 — modèles miroirs du contrat API (backend) pour l'outil
 * décisionnel « Regroupement familial art. 10bis (BE) » (F-IM-27-regroupement-10bis-be).
 *
 * BELGIQUE uniquement — scoring d'éligibilité pour le regroupement familial
 * sous l'art. 10bis de la Loi du 15/12/1980 sur l'accès au territoire, le
 * séjour, l'établissement et l'éloignement des étrangers. À la différence de
 * l'art. 10ter (regroupant titulaire d'un séjour illimité — carte B ou C),
 * l'art. 10bis vise le regroupement d'un étranger admis à un séjour limité
 * (carte A). Conséquence pratique : condition supplémentaire de validité de
 * la carte A à la date d'analyse (champ `dateFinCarteA`).
 *
 * Seuil de ressources : 120 % du Revenu d'intégration sociale (RIS) — AR du
 * 17/05/2007 — seuil tarifaire opérationnel : 1 500 €/mois (référence outil,
 * valeur indicative miroir 10ter).
 */

export type Regroupement10bisLienFamilial =
  | 'CONJOINT'
  | 'PARTENAIRE_ENREGISTRE'
  | 'ENFANT_MOINS_21'
  | 'ENFANT_21_PLUS_CHARGE'
  | 'ASCENDANT_CHARGE';

/**
 * SF-215-06 : art. 10bis = regroupement vers un titulaire d'un séjour LIMITÉ
 * uniquement (carte A). Une seule valeur possible — typeCarte sera affiché en
 * lecture seule côté UI mais le champ est conservé dans le contrat API par
 * symétrie avec l'art. 10ter.
 */
export type Regroupement10bisTypeCarte = 'CARTE_A';

export type Regroupement10bisVerdict = 'ELIGIBLE' | 'SOUS_RESERVE' | 'INELIGIBLE';

export interface Regroupement10bisBeRequest {
  lienFamilial: Regroupement10bisLienFamilial;
  typeCarteRegroupant: Regroupement10bisTypeCarte;
  revenusMensuelsNetsRegroupant: number; // entier 0-100 000
  dureeSejour: number;                    // entier mois 0-600
  dateFinCarteA: string;                  // ISO date YYYY-MM-DD — NOUVEAU vs 10ter
  logementConforme: boolean;
  assuranceMaladie: boolean;
  menaceOrdrePublic: boolean;
}

export interface Regroupement10bisBeResponse {
  caseFileId: string;
  lienFamilial: string;
  typeCarteRegroupant: string;
  revenusMensuelsNetsRegroupant: number;
  dureeSejour: number;
  dateFinCarteA: string;          // NOUVEAU vs 10ter
  logementConforme: boolean;
  assuranceMaladie: boolean;
  menaceOrdrePublic: boolean;
  seuilRessources: number;        // 1500
  differentielRevenus: number;    // signé : revenus - seuil
  scoreEligibilite: number;       // 0-100
  verdict: Regroupement10bisVerdict;
  conditionTitreEnCours: boolean; // NOUVEAU vs 10ter — !carteAExpiree
  criteresNonRemplis: string[];
  baseJuridique: string;
}

export interface Regroupement10bisLienFamilialOption {
  code: Regroupement10bisLienFamilial;
  label: string;
}

export interface Regroupement10bisTypeCarteOption {
  code: Regroupement10bisTypeCarte;
  label: string;
}

export const REGROUPEMENT_10BIS_LIENS_FAMILIAUX: ReadonlyArray<Regroupement10bisLienFamilialOption> = [
  { code: 'CONJOINT', label: 'Conjoint' },
  { code: 'PARTENAIRE_ENREGISTRE', label: 'Partenaire enregistré' },
  { code: 'ENFANT_MOINS_21', label: 'Enfant de moins de 21 ans' },
  { code: 'ENFANT_21_PLUS_CHARGE', label: 'Enfant majeur à charge' },
  { code: 'ASCENDANT_CHARGE', label: 'Ascendant à charge' },
];

/**
 * SF-215-06 : 1 seule valeur — art. 10bis vise exclusivement le regroupement
 * vers un séjour limité (carte A).
 */
export const REGROUPEMENT_10BIS_TYPES_CARTE: ReadonlyArray<Regroupement10bisTypeCarteOption> = [
  { code: 'CARTE_A', label: 'Carte A (séjour limité)' },
];

/** Seuil opérationnel de ressources mensuelles nettes (AR 17/05/2007). */
export const SEUIL_RESSOURCES_DEFAULT_10BIS = 1_500;
