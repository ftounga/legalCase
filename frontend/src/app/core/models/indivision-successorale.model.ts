/**
 * SF-FA-24-12 : modèles TypeScript pour l'outil décisionnel "Indivision
 * successorale" (F-FA-24). FR uniquement — art. 815 à 832-2 + 1873-1 et s. +
 * 815-1 et s. Cciv. Contrat figé dans SF-FA-24-11 (backend, mergé PR #681).
 */

/** Type de régime d'indivision successorale (3 cas — art. 815 / 1873-1 / 815-1). */
export type TypeIndivisionSuccessorale =
  | 'INDIVISION_LEGALE'
  | 'INDIVISION_CONVENTIONNELLE'
  | 'MAINTIEN_FORCE';

/** Verdict de gestion. */
export type VerdictGestionIndivision = 'HARMONIEUSE' | 'CONFLICTUELLE' | 'BLOCAGE';

/** Code dispositif recommandé (aligné sur les constantes calculator). */
export type DispositifRecommandeIndivision =
  | 'CONVENTION_INDIVISION_5_ANS'
  | 'MAINTIEN_INDIVISION_LEGALE'
  | 'MEDIATION_FAMILIALE'
  | 'PARTAGE_AMIABLE'
  | 'PARTAGE_JUDICIAIRE'
  | 'MAINTIEN_FORCE_PRESERVE';

/** Libellés humains des types d'indivision (radio). */
export const TYPE_INDIVISION_SUCCESSORALE_LABELS:
    Readonly<Record<TypeIndivisionSuccessorale, string>> = {
  INDIVISION_LEGALE: 'Indivision légale (art. 815 Cciv)',
  INDIVISION_CONVENTIONNELLE: 'Indivision conventionnelle (art. 1873-1 Cciv)',
  MAINTIEN_FORCE: 'Maintien forcé judiciaire (art. 815-1 Cciv)',
};

/** Libellés humains des verdicts. */
export const VERDICT_GESTION_INDIVISION_LABELS:
    Readonly<Record<VerdictGestionIndivision, string>> = {
  HARMONIEUSE: 'Gestion harmonieuse',
  CONFLICTUELLE: 'Gestion conflictuelle',
  BLOCAGE: 'Blocage caractérisé',
};

/** Libellés humains des dispositifs recommandés. */
export const DISPOSITIF_RECOMMANDE_INDIVISION_LABELS:
    Readonly<Record<DispositifRecommandeIndivision, string>> = {
  CONVENTION_INDIVISION_5_ANS: 'Convention d\'indivision (5 ans, art. 1873-1+)',
  MAINTIEN_INDIVISION_LEGALE: 'Maintien de l\'indivision légale',
  MEDIATION_FAMILIALE: 'Médiation familiale (art. 1108 CPC — préalable)',
  PARTAGE_AMIABLE: 'Partage amiable (notarié)',
  PARTAGE_JUDICIAIRE: 'Partage judiciaire (art. 1364 CPC)',
  MAINTIEN_FORCE_PRESERVE: 'Maintien forcé préservé (art. 815-1+)',
};

/**
 * Requête d'analyse — alignée sur le record backend
 * `IndivisionSuccessoraleRequest`. Tous les booleans sont nullable côté
 * frontend (radios `Oui`/`Non`).
 */
export interface IndivisionSuccessoraleRequest {
  dateOuvertureSuccession: string; // ISO YYYY-MM-DD
  typeIndivision: TypeIndivisionSuccessorale;
  nbHeritiers: number;
  valeurPatrimoineIndivisEur: number;
  valeurBienOccupeEur: number | null;
  consentementsTous: boolean;
  occupationExclusive: boolean;
  actesAdministrationContestes: boolean;
  demandePartage: boolean;
}

/** Réponse de l'endpoint d'analyse. */
export interface IndivisionSuccessoraleResponse {
  caseFileId: string;
  dateOuvertureSuccession: string;
  typeIndivision: TypeIndivisionSuccessorale;
  nbHeritiers: number;
  valeurPatrimoineIndivisEur: number;
  valeurBienOccupeEur: number;
  consentementsTous: boolean;
  occupationExclusive: boolean;
  actesAdministrationContestes: boolean;
  demandePartage: boolean;
  dureeIndivisionMois: number;
  verdictGestion: VerdictGestionIndivision;
  dispositifRecommande: DispositifRecommandeIndivision | string;
  indemniteOccupationDue: boolean;
  indemniteOccupationDueEur: number;
  fraisGestionEstimesEur: number;
  scoreConflictualite: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: string;
}
