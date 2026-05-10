/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Heures supplémentaires" (FR).
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Logique miroir (FRANCE uniquement) :
 *   `tauxHoraireBrut         ← round(salaireBrutMensuel / HEURES_MOIS_FR, 2)`
 *   `heuresSupDeclarees25pct ← aiData.heuresSupMentionneesDansDossier.totalDeclarees25pct`
 *   `heuresSupDeclarees50pct ← aiData.heuresSupMentionneesDansDossier.totalDeclarees50pct`
 *   `heuresHorsContingent    ← aiData.heuresSupMentionneesDansDossier.horsContingent`
 */

export const HEURES_MOIS_FR = 151.67;

export interface HeuresSupPrefillInput {
  aiData?: {
    salaireBrutMensuel?: number | null;
    heuresSupMentionneesDansDossier?: {
      totalDeclarees25pct?: number | null;
      totalDeclarees50pct?: number | null;
      horsContingent?: number | null;
    } | null;
  } | null;
  workspaceCountry?: string;
}

function gateFrance<T>(input: HeuresSupPrefillInput, fn: () => T): T | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  return fn();
}

export function computeTauxHoraireBrut(input: HeuresSupPrefillInput): number | null {
  return gateFrance(input, () => {
    const v = input.aiData?.salaireBrutMensuel;
    if (typeof v !== 'number' || v <= 0) return null;
    return Math.round((v / HEURES_MOIS_FR) * 100) / 100;
  });
}

export function computeHeures25(input: HeuresSupPrefillInput): number | null {
  return gateFrance(input, () => {
    const v = input.aiData?.heuresSupMentionneesDansDossier?.totalDeclarees25pct;
    return typeof v === 'number' ? v : null;
  });
}

export function computeHeures50(input: HeuresSupPrefillInput): number | null {
  return gateFrance(input, () => {
    const v = input.aiData?.heuresSupMentionneesDansDossier?.totalDeclarees50pct;
    return typeof v === 'number' ? v : null;
  });
}

export function computeHeuresHorsContingent(input: HeuresSupPrefillInput): number | null {
  return gateFrance(input, () => {
    const v = input.aiData?.heuresSupMentionneesDansDossier?.horsContingent;
    return typeof v === 'number' ? v : null;
  });
}

export function computePrefillCount(input: HeuresSupPrefillInput): number {
  let count = 0;
  if (computeTauxHoraireBrut(input) !== null) count++;
  if (computeHeures25(input) !== null) count++;
  if (computeHeures50(input) !== null) count++;
  if (computeHeuresHorsContingent(input) !== null) count++;
  return count;
}

export const HeuresSupSectionPrefillRules = {
  computeTauxHoraireBrut,
  computeHeures25,
  computeHeures50,
  computeHeuresHorsContingent,
  computePrefillCount,
};
