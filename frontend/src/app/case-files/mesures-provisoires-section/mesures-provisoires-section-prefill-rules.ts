/**
 * F-236 SF-236-02 — Helper partagé `MesuresProvisoiresPrefillRules`.
 *
 * 5 champs : dateAudienceAOMP, revenusDemandeur, revenusDefendeur,
 * violencesAlleguees, patrimoineCommunSignificatif.
 */
import { MesuresProvisoiresAiData } from '../../core/models/mesures-provisoires.model';

export const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

export interface MesuresProvisoiresPrefillInput {
  aiData?: MesuresProvisoiresAiData | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computeDateAudienceAOMP(input: MesuresProvisoiresPrefillInput): string | null {
  const v = input.aiData?.dateAudienceAOMP;
  return typeof v === 'string' && v.length > 0 && ISO_DATE_REGEX.test(v) ? v : null;
}

/** Rétro-compat : mensuel direct (priorité) ou annuel /12. */
export function computeRevenusDemandeur(input: MesuresProvisoiresPrefillInput): number | null {
  const ai = input.aiData;
  if (!ai) return null;
  if (typeof ai.revenusEpouxDemandeurEur === 'number' && ai.revenusEpouxDemandeurEur >= 0) {
    return ai.revenusEpouxDemandeurEur;
  }
  if (typeof ai.revenusAnnuelsEpoux1Eur === 'number' && ai.revenusAnnuelsEpoux1Eur >= 0) {
    return Math.round(ai.revenusAnnuelsEpoux1Eur / 12);
  }
  return null;
}

export function computeRevenusDefendeur(input: MesuresProvisoiresPrefillInput): number | null {
  const ai = input.aiData;
  if (!ai) return null;
  if (typeof ai.revenusEpouxDefendeurEur === 'number' && ai.revenusEpouxDefendeurEur >= 0) {
    return ai.revenusEpouxDefendeurEur;
  }
  if (typeof ai.revenusAnnuelsEpoux2Eur === 'number' && ai.revenusAnnuelsEpoux2Eur >= 0) {
    return Math.round(ai.revenusAnnuelsEpoux2Eur / 12);
  }
  return null;
}

export function computeViolencesAlleguees(input: MesuresProvisoiresPrefillInput): boolean | null {
  const v = input.aiData?.violencesAlleguees;
  return typeof v === 'boolean' ? v : null;
}

export function computePatrimoineCommun(input: MesuresProvisoiresPrefillInput): boolean | null {
  const v = input.aiData?.patrimoineCommunSignificatif;
  return typeof v === 'boolean' ? v : null;
}

export function computePrefillCount(input: MesuresProvisoiresPrefillInput): number {
  let n = 0;
  if (computeDateAudienceAOMP(input) !== null) n++;
  if (computeRevenusDemandeur(input) !== null) n++;
  if (computeRevenusDefendeur(input) !== null) n++;
  if (computeViolencesAlleguees(input) !== null) n++;
  if (computePatrimoineCommun(input) !== null) n++;
  return n;
}

export const MesuresProvisoiresPrefillRules = {
  ISO_DATE_REGEX,
  computeDateAudienceAOMP,
  computeRevenusDemandeur,
  computeRevenusDefendeur,
  computeViolencesAlleguees,
  computePatrimoineCommun,
  computePrefillCount,
};
