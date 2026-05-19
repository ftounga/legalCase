/**
 * F-236 SF-236-02 — Helper partagé `MesuresProvisoiresPrefillRules`.
 *
 * 5 champs : dateAudienceAOMP, revenusDemandeur, revenusDefendeur,
 * violencesAlleguees, patrimoineCommunSignificatif.
 *
 * SF-246-08 :
 * - `dateAudienceAOMP` désormais champ réel (`vie_commune_detection`).
 * - `computePatrimoineCommun` : dérive en `true` si `patrimoineCommunEur > 0`
 *   (§5.2), sinon fallback sur boolean `patrimoineCommunSignificatif`.
 * - `computeRevenusDemandeur` : priorité `revenusAnnuelsEpoux` (nouveau champ réel
 *   annuel / 12) sur `revenusAnnuelsEpoux1Eur` (rétro-compat).
 * - Ajout de la garde BE : retourne 0 hors France.
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

/** Rétro-compat : annuel réel (SF-246-08, priorité), mensuel direct ou annuel rétro /12. */
export function computeRevenusDemandeur(input: MesuresProvisoiresPrefillInput): number | null {
  const ai = input.aiData;
  if (!ai) return null;
  // SF-246-08 : nouveau champ réel annuel — priorité absolue.
  if (typeof ai.revenusAnnuelsEpoux === 'number' && ai.revenusAnnuelsEpoux > 0) {
    return Math.round(ai.revenusAnnuelsEpoux / 12);
  }
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

/**
 * SF-246-08 : dérivé depuis `patrimoineCommunEur > 0` (présence d'un montant
 * réel = patrimoine commun significatif). Fallback sur boolean
 * `patrimoineCommunSignificatif` pour rétro-compat.
 */
export function computePatrimoineCommun(input: MesuresProvisoiresPrefillInput): boolean | null {
  const eur = input.aiData?.patrimoineCommunEur;
  if (typeof eur === 'number' && eur > 0) return true;
  const v = input.aiData?.patrimoineCommunSignificatif;
  return typeof v === 'boolean' ? v : null;
}

export function computePrefillCount(input: MesuresProvisoiresPrefillInput): number {
  if (input.workspaceCountry && input.workspaceCountry !== 'FRANCE') return 0;
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
