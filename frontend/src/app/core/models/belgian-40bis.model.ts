/**
 * SF-IM-14-07 : types frontend pour l'outil décisionnel
 * "Regroupement familial citoyen UE — art. 40bis Loi 15/12/1980"
 * (carte F). BE uniquement. Contrat figé en SF-IM-14-03 backend
 * (PR #510). L'équivalent FR (regroupement familial CESEDA) est
 * couvert par F-IM-03 / F-IM-12 ; distinct de l'art. 40ter
 * (regroupant Belge — règles plus strictes, voir SF-IM-14-04 + 08).
 */

/** Liens familiaux acceptés au titre de l'art. 40bis §2 Loi 15/12/1980. */
export type LienFamilial =
  | 'CONJOINT'
  | 'PARTENAIRE_ENREGISTRE'
  | 'DESCENDANT_MINEUR'
  | 'DESCENDANT_MAJEUR_CHARGE'
  | 'ASCENDANT_CHARGE';

/** Catégories d'activité du regroupant — directive 2004/38/CE. */
export type RegroupantActiviteCategorie =
  | 'TRAVAILLEUR'
  | 'ETUDIANT'
  | 'INACTIF_AVEC_RESSOURCES'
  | 'AUTRE';

/** Verdict probabilité d'octroi (seuils 75 / 50). */
export type VerdictProbabilite = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

export interface LienFamilialOption {
  code: LienFamilial;
  label: string;
}

export interface RegroupantActiviteCategorieOption {
  code: RegroupantActiviteCategorie;
  label: string;
}

/** Référentiel UI — labels affichés dans le mat-select. */
export const LIENS_FAMILIAUX: LienFamilialOption[] = [
  { code: 'CONJOINT',                 label: 'Conjoint' },
  { code: 'PARTENAIRE_ENREGISTRE',    label: 'Partenaire enregistré (cohabitation légale)' },
  { code: 'DESCENDANT_MINEUR',        label: 'Descendant mineur' },
  { code: 'DESCENDANT_MAJEUR_CHARGE', label: 'Descendant majeur à charge' },
  { code: 'ASCENDANT_CHARGE',         label: 'Ascendant à charge' },
];

export const REGROUPANT_ACTIVITES: RegroupantActiviteCategorieOption[] = [
  { code: 'TRAVAILLEUR',              label: 'Travailleur (salarié ou indépendant)' },
  { code: 'ETUDIANT',                 label: 'Étudiant' },
  { code: 'INACTIF_AVEC_RESSOURCES',  label: 'Inactif avec ressources suffisantes' },
  { code: 'AUTRE',                    label: 'Autre / non précisé' },
];

export interface Belgian40bisRequest {
  lienFamilial: LienFamilial;
  regroupantCitoyenUe: boolean;
  regroupantActiviteCategorie: RegroupantActiviteCategorie;
  ressourcesSuffisantes: boolean;
  assuranceMaladieUe: boolean;
  logementSuffisant: boolean;
  menaceOrdrePublic: boolean;
  /** Optionnel — YYYY-MM-DD. */
  dateDepotDemande?: string | null;
}

export interface Belgian40bisResponse {
  caseFileId: string;
  lienFamilial: LienFamilial;
  regroupantCitoyenUe: boolean;
  regroupantActiviteCategorie: RegroupantActiviteCategorie;
  ressourcesSuffisantes: boolean;
  assuranceMaladieUe: boolean;
  logementSuffisant: boolean;
  menaceOrdrePublic: boolean;
  dateDepotDemande: string | null;
  country: string;
  lienValide: boolean;
  regroupantValide: boolean;
  ressourcesOk: boolean;
  assuranceOk: boolean;
  logementOk: boolean;
  pasMenace: boolean;
  /** 0-100. */
  scoreGlobal: number;
  verdictProbabilite: VerdictProbabilite;
  criteresNonRemplis: string[];
  /** Date dépôt + 6 mois (art. 42 §1 Loi 15/12/1980). */
  dateExpirationInstruction: string | null;
  formule: string;
  baseJuridique: string;
  messages: string[];
}
