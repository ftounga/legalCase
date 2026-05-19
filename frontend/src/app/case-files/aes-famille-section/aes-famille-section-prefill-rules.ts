import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-236-02 / SF-246-18 — Helper partagé pour `AesFamilleSectionComponent`
 * (F-IM-09-aes-famille) — FR mono-pays.
 *
 * SF-236-02 : 2 champs — dateEntreeFrance + dureePresenceMois (dérivé).
 * SF-246-18 : 2 nouveaux champs — dureeScolaritePlusAncienEnfant + dateDepotDemande.
 *   Total : 4 champs pré-remplissables.
 *
 * Sources backend (ImmigrationExtractedData) :
 *   - dateEntreeFrance ← `aesDateEntreeFrance` (champ typé, SF-246-18)
 *   - dureePresenceMois ← calculé ici depuis aesDateEntreeFrance
 *   - dureeScolaritePlusAncienEnfant ← `aesDureeScolaritePlusAncienEnfantAnnees`
 *   - dateDepotDemande ← `dateDepotProcedure` (existant)
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

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
    // SF-246-18 : lecture sur champ typé `aesDateEntreeFrance`.
    const v = ai.aesDateEntreeFrance;
    if (typeof v !== 'string' || v.length < 10) return null;
    return v.substring(0, 10);
  },

  computeDureePresenceMois(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const date = this.computeDateEntreeFrance(input);
    if (date === null) return null;
    return computeMonthsSince(date);
  },

  /** SF-246-18 : durée de scolarité de l'enfant le plus ancien (années entières ≥ 0). */
  computeDureeScolaritePlusAncienEnfant(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.aesDureeScolaritePlusAncienEnfantAnnees;
    if (typeof v !== 'number' || !Number.isInteger(v) || v < 0 || v > 30) return null;
    return v;
  },

  /** SF-246-18 : date de dépôt de la demande depuis dateDepotProcedure (ISO non future). */
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
    if (this.computeDureePresenceMois(input) !== null) n++;
    // SF-246-18 : 2 nouveaux champs
    if (this.computeDureeScolaritePlusAncienEnfant(input) !== null) n++;
    if (this.computeDateDepotDemande(input) !== null) n++;
    return n;
  },
} as const;
