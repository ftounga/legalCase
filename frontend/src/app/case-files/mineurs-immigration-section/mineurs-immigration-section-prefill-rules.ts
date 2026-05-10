import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * F-236 SF-236-02 — Helper partagé pour `MineursImmigrationSectionComponent`
 * (F-IM-19-mineurs) — FR mono-pays.
 *
 * 3 champs : dateNaissance, dateEntreeFrance, nationalite (string).
 */

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

function nonEmptyString(v: unknown): string | null {
  return typeof v === 'string' && v.length > 0 ? v : null;
}

export const MineursImmigrationPrefillRules = {
  computeDateNaissance(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    if (!input.aiData) return null;
    return nonEmptyString((input.aiData as any).dateNaissance);
  },

  computeDateEntreeFrance(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    if (!input.aiData) return null;
    return nonEmptyString((input.aiData as any).dateEntreeFrance);
  },

  computeNationalite(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    if (!input.aiData) return null;
    return nonEmptyString((input.aiData as any).nationalite);
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateNaissance(input) !== null) n++;
    if (this.computeDateEntreeFrance(input) !== null) n++;
    if (this.computeNationalite(input) !== null) n++;
    return n;
  },
} as const;
