import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-214-14 — Helper partagé pour {@link RenouvellementDelaiSectionComponent}
 * (F-IM-31-renouvellement-delai-depot-fr) — FR mono-pays.
 *
 * Champs pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - dateExpirationTitre : `aiData.dateExpirationTitre` (ISO YYYY-MM-DD).
 *  - typeTitre : `aiData.typeTitreSejour` (texte libre non vide).
 *
 * Champs NON pré-remplis :
 *  - dateDepotDossier : pas de champ IA dédié, saisi par l'avocat si déjà déposé.
 *
 * Total : jusqu'à 2 champs pre-remplissables (sur 3 saisissables).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const RenouvellementDelaiPrefillRules = {

  computeDateExpirationTitre(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.dateExpirationTitre;
    if (typeof v !== 'string') return null;
    const trimmed = v.trim();
    if (!ISO_DATE_RE.test(trimmed)) return null;
    return trimmed;
  },

  computeTypeTitre(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.typeTitreSejour;
    if (typeof v !== 'string') return null;
    const trimmed = v.trim();
    if (trimmed.length === 0) return null;
    return trimmed;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateExpirationTitre(input) !== null) n++;
    if (this.computeTypeTitre(input) !== null) n++;
    return n;
  },
} as const;
