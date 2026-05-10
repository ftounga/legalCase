import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * F-236 SF-236-02 — Helper partagé pour `AesHumanitaireSectionComponent`
 * (F-IM-09-aes-humanitaire) — FR mono-pays.
 *
 * 2 champs : dateEntreeFrance (fallback non typé) + dateDepotDemande
 * (depuis aiData.dateDepotProcedure, doit être >= dateEntreeFrance).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const AesHumanitairePrefillRules = {
  ISO_DATE_RE,

  computeDateEntreeFrance(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = (ai as { dateEntreeFrance?: string | null }).dateEntreeFrance;
    if (typeof v !== 'string' || v.length < 10) return null;
    return v.substring(0, 10);
  },

  /**
   * dateDepotDemande : posée si dépôt ISO non futur ET (pas de
   * dateEntreeFrance OU depot >= dateEntreeFrance).
   */
  computeDateDepotDemande(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const depot = ai.dateDepotProcedure;
    if (typeof depot !== 'string' || !ISO_DATE_RE.test(depot)) return null;
    if (depot > todayIso()) return null;
    const entree = this.computeDateEntreeFrance(input);
    if (entree !== null && depot < entree) return null;
    return depot;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateEntreeFrance(input) !== null) n++;
    if (this.computeDateDepotDemande(input) !== null) n++;
    return n;
  },
} as const;
