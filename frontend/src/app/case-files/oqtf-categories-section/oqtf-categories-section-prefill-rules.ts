import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-214-10 — Helper partagé pour {@link OqtfCategoriesSectionComponent}
 * (F-IM-29-oqtf-categories-l6111-fr) — FR mono-pays.
 *
 * Champs pre-fill (depuis {@link ImmigrationExtractedData}, réutilise les champs
 * OQTF existants — aucun nouveau champ ajouté) :
 *  - dateNotificationOqtf : `aiData.dateNotificationOqtf` (ISO yyyy-MM-dd, non future).
 *  - motifOqtf (textarea) : dérivé de `aiData.motifOqtfCode` (enum) → libellé lisible.
 *
 * Champs NON pré-remplis :
 *  - categorieL611 : pas de champ IA dédié, choisi par l'avocat (qualification juridique).
 *
 * Total : 2 champs pre-remplissables (sur 3 saisissables).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export type MotifOqtfCode =
  | 'REFUS_TITRE'
  | 'EXPIRATION_TITRE'
  | 'SEJOUR_IRREGULIER'
  | 'RETRAIT_TITRE'
  | 'AUTRE';

export const MOTIF_OQTF_LIBELLES: Readonly<Record<MotifOqtfCode, string>> = {
  REFUS_TITRE: 'Refus de titre de séjour',
  EXPIRATION_TITRE: 'Expiration du titre de séjour',
  SEJOUR_IRREGULIER: 'Séjour irrégulier',
  RETRAIT_TITRE: 'Retrait du titre de séjour',
  AUTRE: 'Autre motif',
};

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const OqtfCategoriesPrefillRules = {
  ISO_DATE_RE,
  MOTIF_OQTF_LIBELLES,

  computeDateNotificationOqtf(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const date = ai.dateNotificationOqtf;
    if (typeof date !== 'string' || !ISO_DATE_RE.test(date)) return null;
    if (date > todayIso()) return null;
    return date;
  },

  computeMotifOqtf(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const code = ai.motifOqtfCode;
    if (!code) return null;
    const libelle = MOTIF_OQTF_LIBELLES[code as MotifOqtfCode];
    return libelle ?? null;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateNotificationOqtf(input) !== null) n++;
    if (this.computeMotifOqtf(input) !== null) n++;
    return n;
  },
} as const;
