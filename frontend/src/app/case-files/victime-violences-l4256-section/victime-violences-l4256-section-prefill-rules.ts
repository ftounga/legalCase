import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-208-08 — Helper partage pour {@link VictimeViolencesL4256SectionComponent}
 * (F-IM-24-victime-violences-l4256-fr) — FR mono-pays.
 *
 * Pre-fill IA partiel :
 *  - dateOrdonnanceProtection : pas de champ dedie dans `ImmigrationExtractedData`
 *    V1 (la detection d'une ordonnance JAF est typiquement dans `procedureChecks`
 *    ou les pieces). Pre-fill omis tant que `aiData.dateOrdonnanceProtectionJaf`
 *    n'est pas extrait (gap documente dans la SF — articulation future F-FA-14).
 *
 * Aucun champ pre-rempli actuellement — getPrefillCount renvoie 0. Le composant
 * conserve la structure complete (signals, F-IA-03, getPrefillCount mirror) pour
 * permettre l'evolution future quand l'extraction IA des ordonnances JAF sera
 * ajoutee (F-FA-14 / extension Sonnet).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const VictimeViolencesL4256PrefillRules = {
  ISO_DATE_RE,

  /**
   * Date d'ordonnance de protection — non extraite par l'IA Immigration V1
   * (gap documente : `ImmigrationExtractedData` ne contient pas
   * `dateOrdonnanceProtectionJaf`). Retourne `null` jusqu'a l'enrichissement
   * de l'extraction IA (articulation future avec F-FA-14 ordonnance JAF).
   */
  computeDateOrdonnanceProtection(_input: PrefillCountInput): string | null {
    return null;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateOrdonnanceProtection(input) !== null) n++;
    return n;
  },
} as const;
