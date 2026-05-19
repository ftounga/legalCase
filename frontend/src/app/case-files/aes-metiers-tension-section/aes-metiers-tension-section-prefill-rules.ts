import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-236-02 / SF-246-18 — Helper partagé pour le pré-remplissage IA de
 * `AesMetiersTensionSectionComponent` (F-IM-09-aes-metiers-tension).
 *
 * SF-236-02 : 1 champ — dateDepotDemande (depuis dateDepotProcedure).
 * SF-246-18 : 3 nouveaux champs — dateEntreeFrance + moisActiviteSalariee
 *   + codeMetier. Total : 4 champs pré-remplissables.
 *
 * Sources backend (ImmigrationExtractedData) :
 *   - dateDepotDemande ← `dateDepotProcedure` (existant)
 *   - dateEntreeFrance ← `aesDateEntreeFrance` (champ typé, SF-246-18)
 *   - moisActiviteSalariee ← `aesMoisActiviteSalariee` (0–24, SF-246-18)
 *   - codeMetier ← `aesCodeMetier` (texte libre, SF-246-18)
 *
 * Module pur : pas d'import Angular, pas d'effet de bord. Consommé par
 * `prefillFromAi()` runtime ET le static `getPrefillCount`.
 */
const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const AesMetiersTensionPrefillRules = {
  ISO_DATE_RE,

  /**
   * dateDepotDemande : posée si `aiData.dateDepotProcedure` est une string
   * ISO valide non future ET workspace France. Court-circuite hors France
   * (outil mono-pays).
   */
  computeDateDepotDemande(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const depot = ai.dateDepotProcedure;
    if (typeof depot !== 'string' || !ISO_DATE_RE.test(depot)) return null;
    if (depot > todayIso()) return null;
    return depot;
  },

  /** SF-246-18 : date d'entrée en France (ISO non future, depuis aesDateEntreeFrance). */
  computeDateEntreeFrance(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.aesDateEntreeFrance;
    if (typeof v !== 'string' || !ISO_DATE_RE.test(v)) return null;
    if (v > todayIso()) return null;
    return v;
  },

  /** SF-246-18 : mois d'activité salariée dans les 24 derniers mois (entier 0–24). */
  computeMoisActiviteSalariee(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.aesMoisActiviteSalariee;
    if (typeof v !== 'number' || !Number.isInteger(v) || v < 0 || v > 24) return null;
    return v;
  },

  /** SF-246-18 : code ROME ou libellé du métier en tension (texte non vide). */
  computeCodeMetier(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.aesCodeMetier;
    if (typeof v !== 'string') return null;
    const trimmed = v.trim();
    return trimmed.length > 0 ? trimmed : null;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateDepotDemande(input) !== null) n++;
    // SF-246-18 : 3 nouveaux champs
    if (this.computeDateEntreeFrance(input) !== null) n++;
    if (this.computeMoisActiviteSalariee(input) !== null) n++;
    if (this.computeCodeMetier(input) !== null) n++;
    return n;
  },
} as const;
