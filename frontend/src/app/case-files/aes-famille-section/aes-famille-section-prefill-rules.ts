import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * F-236 SF-236-02 — Helper partagé pour `AesFamilleSectionComponent`
 * (F-IM-09-aes-famille) — FR mono-pays.
 *
 * 2 champs : dateEntreeFrance (depuis aiData.dateEntreeFrance non typé,
 * cast à string >= 10 chars), dureePresenceMois (calculé depuis cette date).
 *
 * NOTE : la date `dateEntreeFrance` n'est pas typée sur
 * `ImmigrationExtractedData` actuellement — fallback gracieux via cast.
 */

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

/** Mois écoulés depuis une date ISO (ou null si malformée). */
export function computeMonthsSince(isoDate: string, now: Date = new Date()): number | null {
  const d = new Date(isoDate);
  if (isNaN(d.getTime())) return null;
  const months = (now.getFullYear() - d.getFullYear()) * 12 + (now.getMonth() - d.getMonth());
  return Math.max(0, months);
}

export const AesFamillePrefillRules = {
  computeMonthsSince,

  computeDateEntreeFrance(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = (ai as { dateEntreeFrance?: string | null }).dateEntreeFrance;
    if (typeof v !== 'string' || v.length < 10) return null;
    return v.substring(0, 10);
  },

  computeDureePresenceMois(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const date = this.computeDateEntreeFrance(input);
    if (date === null) return null;
    return computeMonthsSince(date);
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateEntreeFrance(input) !== null) n++;
    if (this.computeDureePresenceMois(input) !== null) n++;
    return n;
  },
} as const;
