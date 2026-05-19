import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * F-236 SF-236-02 — Helper partagé pour `MineursImmigrationSectionComponent`
 * (F-IM-19-mineurs) — FR mono-pays.
 *
 * 3 champs : dateNaissance (SF-246-19 — typé), dateEntreeFrance
 * (SF-246-18 via `aesDateEntreeFrance` — typé), nationalite (F-235 — typé).
 *
 * SF-246-19 : casts `as any` supprimés — accès typé direct sur
 * `ImmigrationExtractedData` pour les 3 champs.
 */

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

function nonEmptyString(v: unknown): string | null {
  return typeof v === 'string' && v.length > 0 ? v : null;
}

export const MineursImmigrationPrefillRules = {
  /** Date de naissance du mineur — depuis `aiData.mineursDateNaissance` (ISO non-future). */
  computeDateNaissance(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    if (!input.aiData) return null;
    return nonEmptyString(input.aiData.mineursDateNaissance);
  },

  /**
   * Date d'entrée en France du mineur — réutilise `aiData.aesDateEntreeFrance`
   * (SF-246-18, typé). Suppression du cast `as any` antérieur.
   */
  computeDateEntreeFrance(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    if (!input.aiData) return null;
    return nonEmptyString(input.aiData.aesDateEntreeFrance);
  },

  /**
   * Nationalité du mineur — depuis `aiData.nationalite` (F-235, typé).
   * Suppression du cast `as any` antérieur.
   */
  computeNationalite(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    if (!input.aiData) return null;
    return nonEmptyString(input.aiData.nationalite);
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
