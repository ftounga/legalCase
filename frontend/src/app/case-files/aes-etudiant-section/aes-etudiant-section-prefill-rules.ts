import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * F-236 SF-236-02 — Helper partagé pour `AesEtudiantSectionComponent`
 * (F-IM-09-aes-etudiant) — FR mono-pays.
 *
 * 2 champs : dateEntreeFrance (champ non typé, fallback), dateDepotDemande
 * (depuis aiData.dateDepotProcedure). Tous deux doivent être ISO non futurs.
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const AesEtudiantPrefillRules = {
  ISO_DATE_RE,

  computeDateEntreeFrance(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = (ai as { dateEntreeFrance?: string | null }).dateEntreeFrance;
    if (typeof v !== 'string' || !ISO_DATE_RE.test(v)) return null;
    if (v > todayIso()) return null;
    return v;
  },

  computeDateDepotDemande(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.dateDepotProcedure;
    if (typeof v !== 'string' || !ISO_DATE_RE.test(v)) return null;
    if (v > todayIso()) return null;
    return v;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateEntreeFrance(input) !== null) n++;
    if (this.computeDateDepotDemande(input) !== null) n++;
    return n;
  },
} as const;
