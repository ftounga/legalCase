/**
 * SF-FA-18-10 : modèles TypeScript pour l'outil décisionnel
 * "Adoption" (F-FA-18 — FR uniquement, art. 343-370-2 Cciv).
 *
 * Contrat figé dans SF-FA-18-09 (backend, mergé PR #677).
 */

/** Forme d'adoption (entrée + recommandée en sortie). */
export type FormeAdoption = 'PLENIERE' | 'SIMPLE' | 'AUCUNE';

/** Verdict de recevabilité (scoring niveau 5). */
export type VerdictRecevabiliteAdoption = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

/** Libellés humains pour les formes. */
export const FORME_ADOPTION_LABELS:
  ReadonlyArray<{ code: FormeAdoption; label: string; sub: string }> = [
  {
    code: 'PLENIERE',
    label: 'Adoption plénière (art. 343-359 Cciv)',
    sub: 'Filiation d\'origine effacée, irrévocable, conditions strictes',
  },
  {
    code: 'SIMPLE',
    label: 'Adoption simple (art. 360-370-2 Cciv)',
    sub: 'Double filiation, révocable pour motif grave, conditions souples',
  },
  {
    code: 'AUCUNE',
    label: 'Aucune forme applicable',
    sub: 'Aucune des deux formes n\'est recevable en l\'état',
  },
];

export interface AdoptionRequest {
  formeAdoption: 'PLENIERE' | 'SIMPLE';
  ageAdoptant: number;
  ageAdopte: number;
  consentementParents: boolean;
  consentementAdopte: boolean;
  consentementConjointAdoptant: boolean;
  enquetes: boolean;
  placement6mois: boolean;
  pupilleEtat: boolean;
  adoptantMarie: boolean;
}

export interface AdoptionResponse {
  caseFileId: string;
  formeAdoption: FormeAdoption;
  formeRecommandee: FormeAdoption;
  verdictRecevabilite: VerdictRecevabiliteAdoption;
  ageAdoptant: number;
  ageAdopte: number;
  differenceAgeAns: number;
  criteresNonRemplis: string[];
  delaiInstructionMois: number;
  documentsRequis: string[];
  risqueRefus: string[];
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: string;
}
