/**
 * SF-DT-24-02 : modèles TypeScript pour l'outil décisionnel
 * "Clause de non-concurrence" (Cass. soc. 10/07/2002, art. L.1121-1
 *  Code du travail — FR uniquement).
 *
 * Contrat API figé dans SF-DT-24-01 (backend).
 * Endpoint : POST/GET /api/v1/case-files/{caseFileId}/non-concurrence
 *
 * 4 critères cumulatifs jurisprudentiels (Cass. soc. 10/07/2002) :
 *   (1) limitation territoriale définie
 *   (2) limitation dans le temps
 *   (3) limitation à un objet (activité/fonction) défini
 *   (4) contrepartie financière proportionnée au salaire
 *
 * Pattern jumeau direct frontend : F-DT-23 (requalification intérim → CDI).
 */

/**
 * Verdict de validité produit par le scoring backend (basé sur les 4 critères
 * Cass. soc. 10/07/2002 + ratio contrepartie/salaire).
 */
export type VerdictValiditeNc = 'VALIDE' | 'RISQUE_NULLITE_PARTIELLE' | 'NULLE';

/**
 * Secteur d'activité (impacte les seuils jurisprudentiels de proportionnalité
 * de la contrepartie et l'usage sectoriel — cf. baseJuridique backend).
 */
export type SecteurActivite =
  | 'INFORMATIQUE'
  | 'COMMERCE'
  | 'INDUSTRIE'
  | 'SERVICES'
  | 'AUTRE';

export interface NonConcurrenceRequest {
  clausePresenteContrat: boolean;
  limiteTerritoireDefini: boolean;
  territoireDescription: string;
  limiteDureeDefinie: boolean;
  dureeMois: number;
  limiteObjetDefini: boolean;
  objetDescription: string;
  contrepartieFinancierePresente: boolean;
  contrepartieMontantMensuelEur: number;
  salaireMensuelBrutEur: number;
  secteurActivite: SecteurActivite;
  /** ISO YYYY-MM-DD. */
  datePriseEffet: string;
}

export interface NonConcurrenceResponse {
  caseFileId: string;
  clausePresenteContrat: boolean;
  limiteTerritoireDefini: boolean;
  territoireDescription: string;
  limiteDureeDefinie: boolean;
  dureeMois: number;
  limiteObjetDefini: boolean;
  objetDescription: string;
  contrepartieFinancierePresente: boolean;
  contrepartieMontantMensuelEur: number;
  salaireMensuelBrutEur: number;
  secteurActivite: SecteurActivite;
  datePriseEffet: string;
  critere1TerritoireOk: boolean;
  critere2DureeOk: boolean;
  critere3ObjetOk: boolean;
  critere4ContrepartieOk: boolean;
  ratioContrepartiePct: number;
  scoreValidite: number;
  verdictValidite: VerdictValiditeNc;
  indemniteContrepartieDueEur: number;
  indemnitePotentielleNulliteEur: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}

export interface SecteurActiviteOption {
  code: SecteurActivite;
  label: string;
}

/**
 * 5 secteurs d'activité — option AUTRE comme fallback.
 */
export const SECTEUR_ACTIVITE_OPTIONS: SecteurActiviteOption[] = [
  { code: 'INFORMATIQUE', label: 'Informatique / IT' },
  { code: 'COMMERCE', label: 'Commerce / Distribution' },
  { code: 'INDUSTRIE', label: 'Industrie / Production' },
  { code: 'SERVICES', label: 'Services' },
  { code: 'AUTRE', label: 'Autre secteur' },
];

export function secteurActiviteLabel(code: SecteurActivite): string {
  return SECTEUR_ACTIVITE_OPTIONS.find((o) => o.code === code)?.label ?? code;
}

export function verdictValiditeNcLabel(v: VerdictValiditeNc): string {
  switch (v) {
    case 'VALIDE': return 'Clause valide';
    case 'RISQUE_NULLITE_PARTIELLE': return 'Risque de nullité partielle';
    case 'NULLE': return 'Clause nulle';
  }
}
