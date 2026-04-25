/**
 * SF-FA-19-04 : modèles TypeScript pour l'outil décisionnel
 * "Changement de résidence" (F-FA-19, FRANCE uniquement, art. 373-2 Cciv —
 * obligation d'information préalable, délai de préavis raisonnable,
 * modification éventuelle des modalités de la DVH).
 *
 * Contrat importé du backend SF-FA-19-03.
 */

export type RaisonChangement =
  | 'TRAVAIL'
  | 'FAMILLE'
  | 'LOGEMENT'
  | 'RAPPROCHEMENT_FAMILIAL'
  | 'AUTRE';

export type ModeResidenceCh =
  | 'ALTERNEE'
  | 'EXCLUSIVE_DEMANDEUR'
  | 'EXCLUSIVE_DEFENDEUR';

export type ChangementResidenceVerdict = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

export interface ChangementResidenceRequest {
  /** ISO date YYYY-MM-DD (date prévue du changement). */
  dateChangementPrevu: string;
  distanceKm: number;
  raisonChangement: RaisonChangement;
  consentementAutreParent: boolean;
  informePrealablement: boolean;
  delaiInformationJours: number;
  modeResidenceActuel: ModeResidenceCh;
  ageEnfants: number[];
  scolariteImpactee: boolean;
  modificationDvhDemandee: boolean;
}

export interface ChangementResidenceResponse {
  caseFileId: string;
  dateChangementPrevu: string;
  distanceKm: number;
  raisonChangement: RaisonChangement;
  consentementAutreParent: boolean;
  informePrealablement: boolean;
  delaiInformationJours: number;
  modeResidenceActuel: ModeResidenceCh;
  ageEnfants: number[];
  scolariteImpactee: boolean;
  modificationDvhDemandee: boolean;
  scoreAcceptabilite: number;
  verdictProbabiliteAcceptation: ChangementResidenceVerdict;
  obligationInformationRespectee: boolean;
  expertisePsyEnfantRecommandee: boolean;
  delaiPreavisLegalOk: boolean;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}

export interface RaisonChangementOption {
  code: RaisonChangement;
  label: string;
}

export const RAISONS_CHANGEMENT_FR: RaisonChangementOption[] = [
  { code: 'TRAVAIL', label: 'Mutation / nouvel emploi' },
  { code: 'FAMILLE', label: 'Recomposition familiale' },
  { code: 'LOGEMENT', label: 'Logement (taille / coût / insalubrité)' },
  { code: 'RAPPROCHEMENT_FAMILIAL', label: 'Rapprochement familial' },
  { code: 'AUTRE', label: 'Autre' },
];

export interface ModeResidenceChOption {
  code: ModeResidenceCh;
  label: string;
}

export const MODES_RESIDENCE_FR: ModeResidenceChOption[] = [
  { code: 'ALTERNEE', label: 'Résidence alternée' },
  { code: 'EXCLUSIVE_DEMANDEUR', label: 'Exclusive — chez le demandeur (parent qui déménage)' },
  { code: 'EXCLUSIVE_DEFENDEUR', label: 'Exclusive — chez le défendeur (autre parent)' },
];

export function raisonChangementLabel(code: RaisonChangement): string {
  return RAISONS_CHANGEMENT_FR.find((r) => r.code === code)?.label ?? code;
}

export function modeResidenceLabel(code: ModeResidenceCh): string {
  return MODES_RESIDENCE_FR.find((m) => m.code === code)?.label ?? code;
}

export function verdictChangementResidenceLabel(v: ChangementResidenceVerdict): string {
  switch (v) {
    case 'ELEVEE': return 'Probabilité élevée';
    case 'MOYENNE': return 'Probabilité moyenne';
    case 'FAIBLE': return 'Probabilité faible';
  }
}
