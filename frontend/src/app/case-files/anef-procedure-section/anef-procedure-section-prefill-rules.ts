import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-214-26 — Helper partagé pour {@link AnefProcedureSectionComponent}
 * (F-IM-37-anef-procedure-fr) — FR mono-pays.
 *
 * Champs pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - dateExpirationTitre : `aiData.dateExpirationTitre` (ISO yyyy-MM-dd).
 *  - typeTitreConcerne   : `aiData.typeTitreSejour` (texte libre).
 *
 * Champs NON pré-remplis :
 *  - panneeANEFSignalee : saisi par l'avocat (checkbox).
 *  - dateTentativeDepot : saisi par l'avocat (date de la tentative de dépôt).
 *  - demandeAdresseePrefecture : saisi par l'avocat (checkbox).
 *
 * Total : 2 champs pre-remplissables (sur 5 saisissables).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const AnefProcedurePrefillRules = {
  ISO_DATE_RE,

  computeDateExpirationTitre(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const date = (ai as { dateExpirationTitre?: unknown }).dateExpirationTitre;
    if (typeof date !== 'string' || !ISO_DATE_RE.test(date)) return null;
    return date;
  },

  computeTypeTitreConcerne(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const type = (ai as { typeTitreSejour?: unknown }).typeTitreSejour;
    if (typeof type !== 'string' || type.trim().length === 0) return null;
    return type.trim();
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateExpirationTitre(input) !== null) n++;
    if (this.computeTypeTitreConcerne(input) !== null) n++;
    return n;
  },
} as const;
