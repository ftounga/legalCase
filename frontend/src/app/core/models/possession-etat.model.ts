/**
 * SF-FA-18-08 : modèles TypeScript pour l'outil décisionnel
 * "Possession d'état" (F-FA-18 — FR uniquement,
 * art. 311-1 + 311-2 + 317 Cciv).
 *
 * Contrat figé dans SF-FA-18-07 (backend, mergé PR #670).
 */

/** Verdict de recevabilité (scoring niveau 5). */
export type VerdictRecevabilitePossessionEtat = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

/** Dispositif applicable selon les critères et la durée. */
export type DispositifApplicable =
  | 'CONSTAT_NOTAIRE'
  | 'PREUVE_JUSTICE'
  | 'AUCUN';

/** Libellés humains pour le résultat. */
export const DISPOSITIF_APPLICABLE_LABELS:
  ReadonlyArray<{ code: DispositifApplicable; label: string; sub: string }> = [
  {
    code: 'CONSTAT_NOTAIRE',
    label: 'Constat par notaire (art. 317 Cciv)',
    sub: 'Acte de notoriété — force probante, contestation 5 ans',
  },
  {
    code: 'PREUVE_JUSTICE',
    label: 'Preuve en justice (art. 311-1 + 311-2 Cciv)',
    sub: 'Voie judiciaire — possession invoquée à l\'occasion d\'une action',
  },
  {
    code: 'AUCUN',
    label: 'Aucun dispositif applicable',
    sub: 'Possession d\'état non caractérisée — voies alternatives à étudier',
  },
];

export interface PossessionEtatRequest {
  /** ISO YYYY-MM-DD — obligatoire. */
  dateDebutPossession: string;
  /** ISO YYYY-MM-DD — obligatoire (date du jour si possession en cours). */
  dateFinPossession: string;
  tractatus: boolean;
  fama: boolean;
  /** Facultatif depuis l'ord. n°2005-759 du 4/7/2005. */
  nomen: boolean;
  continueCondition: boolean;
  paisible: boolean;
  nonEquivoque: boolean;
}

export interface PossessionEtatResponse {
  caseFileId: string;
  verdictRecevabilite: VerdictRecevabilitePossessionEtat;
  dispositifApplicable: DispositifApplicable;
  scoreRecevabilite: number;
  dureePossessionAnnees: number;
  delaiContestationActeAns: number;
  delaiContestationCessationAns: number;
  criteresRemplis: string[];
  criteresManquants: string[];
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: string;
}
