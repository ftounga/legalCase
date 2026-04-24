/**
 * SF-IM-14-08 : modèle frontend pour l'outil décisionnel
 * "Regroupement familial d'un Belge" — art. 40ter Loi 15/12/1980.
 * Single-country BE (pas d'équivalent strict en droit français).
 *
 * Contrat aligné sur SF-IM-14-04 (backend mergée PR #511).
 */

/** Lien familial éligible art. 40ter (whitelist Belgian40terCalculator). */
export type LienFamilial =
  | 'CONJOINT'
  | 'PARTENAIRE_LEGAL_ENREGISTRE'
  | 'DESCENDANT_MINEUR'
  | 'DESCENDANT_MAJEUR_CHARGE'
  | 'ASCENDANT_CHARGE_HANDICAP';

/** Verdict de probabilité d'acceptation calculé par le backend. */
export type VerdictProbabilite = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

export interface LienFamilialOption {
  code: LienFamilial;
  label: string;
}

/** Whitelist alignée sur l'enum backend Belgian40terCalculator.isLienValide. */
export const LIENS_FAMILIAUX: LienFamilialOption[] = [
  { code: 'CONJOINT', label: 'Conjoint' },
  { code: 'PARTENAIRE_LEGAL_ENREGISTRE', label: 'Partenaire légalement enregistré' },
  { code: 'DESCENDANT_MINEUR', label: 'Descendant mineur' },
  { code: 'DESCENDANT_MAJEUR_CHARGE', label: 'Descendant majeur à charge' },
  { code: 'ASCENDANT_CHARGE_HANDICAP', label: 'Ascendant à charge handicapé' },
];

/** Seuil de ressources par défaut (120 % RIS — valeur indicative 2025, mensuel net €). */
export const SEUIL_120_PCT_RIS_DEFAULT = 1740;

export interface Belgian40terRequest {
  lienFamilial: LienFamilial;
  regroupantBelge: boolean;
  revenusMensuelsNetsEur: number;
  seuil120PctRisEur: number;
  assuranceMaladie: boolean;
  logementSuffisant: boolean;
  menaceOrdrePublic: boolean;
  dateDepotDemande?: string | null;
}

export interface Belgian40terResponse {
  caseFileId: string;
  lienFamilial: LienFamilial;
  regroupantBelge: boolean;
  revenusMensuelsNetsEur: number;
  seuil120PctRisEur: number;
  assuranceMaladie: boolean;
  logementSuffisant: boolean;
  menaceOrdrePublic: boolean;
  dateDepotDemande: string | null;
  country: 'BELGIQUE';
  lienValide: boolean;
  regroupantBelgeOk: boolean;
  revenusSuffisantsOk: boolean;
  assuranceOk: boolean;
  logementOk: boolean;
  pasMenace: boolean;
  differentielRevenus: number;
  scoreGlobal: number;
  verdictProbabiliteAcceptation: VerdictProbabilite;
  criteresNonRemplis: string[];
  dateExpirationInstructionSiDemande: string | null;
  formule: string;
  baseJuridique: string;
  messages: string[];
}
