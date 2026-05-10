import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * F-236 SF-236-02 — Helper partagé pour le pré-remplissage IA de
 * `AesMetiersTensionSectionComponent` (F-IM-09-aes-metiers-tension).
 *
 * Single-country FR. Mono-champ aujourd'hui (`dateDepotProcedure` →
 * `dateDepotDemande`). Module pur : pas d'import Angular, pas d'effet de
 * bord. Consommé par `prefillFromAi()` runtime ET le static
 * `getPrefillCount` — divergence impossible par construction.
 */
const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

export const AesMetiersTensionPrefillRules = {
  ISO_DATE_RE,

  /**
   * dateDepotDemande : posée si `aiData.dateDepotProcedure` est une string
   * ISO valide non future ET workspace France. Court-circuite hors France
   * (outil mono-pays).
   */
  computeDateDepotDemande(input: PrefillCountInput): string | null {
    if ((input.workspaceCountry ?? 'FRANCE') !== 'FRANCE') return null;
    const ai = input.aiData;
    if (!ai) return null;
    const depot = ai.dateDepotProcedure;
    if (typeof depot !== 'string' || !ISO_DATE_RE.test(depot)) return null;
    if (depot > todayIso()) return null;
    return depot;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if ((input.workspaceCountry ?? 'FRANCE') !== 'FRANCE') return 0;
    let n = 0;
    if (this.computeDateDepotDemande(input) !== null) n++;
    return n;
  },
} as const;
