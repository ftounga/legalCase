import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { CategorieEmploye } from '../../core/models/particulier-employeur-cesu.model';

/**
 * SF-218-14 — Helper partagé pour {@link ParticulierEmployeurCesuSectionComponent}
 * (F-DT-108-particulier-employeur-cesu) — Travail FR mono-pays.
 *
 * Champs pre-fill (depuis {@link TravailExtractedData}) :
 *  - dateEntree : `aiData.dateEntree` (ISO YYYY-MM-DD) — début du contrat.
 *  - dateRupture : `aiData.dateLicenciement` (ISO YYYY-MM-DD) — notification du
 *    licenciement / rupture.
 *  - categorieEmploye : `aiData.cesuCategorieEmploye` — pilote la CCN applicable.
 *
 * Champs NON pré-remplis :
 *  - causeRupture : qualification juridique laissée à l'avocat.
 *  - salaireMensuelMoyen : moyenne 12 (ou 3) derniers mois — assiette propre au
 *    calcul, non factualisable de façon fiable depuis les pièces.
 *  - `particulierEmployeurDetecte` est un FLAG de visibilité (déclenche
 *    l'apparition de l'outil via DecisionToolVisibilityService) — ce n'est PAS
 *    un champ du formulaire, il ne compte donc pas dans le prefill count.
 *
 * Total : 3 champs pre-remplissables (sur 5 saisissables).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

const VALID_CATEGORIES: ReadonlySet<CategorieEmploye> = new Set<CategorieEmploye>([
  'SALARIE_PARTICULIER_EMPLOYEUR',
  'ASSISTANT_MATERNEL',
]);

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

function isoDateOrNull(v: unknown): string | null {
  if (typeof v !== 'string') return null;
  const trimmed = v.trim();
  if (!ISO_DATE_RE.test(trimmed)) return null;
  return trimmed;
}

export const ParticulierEmployeurCesuPrefillRules = {

  computeDateEntree(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull(ai.dateEntree);
  },

  computeDateRupture(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return isoDateOrNull(ai.dateLicenciement);
  },

  computeCategorieEmploye(input: PrefillCountInput): CategorieEmploye | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.cesuCategorieEmploye;
    if (typeof v !== 'string') return null;
    return VALID_CATEGORIES.has(v as CategorieEmploye) ? (v as CategorieEmploye) : null;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateEntree(input) !== null) n++;
    if (this.computeDateRupture(input) !== null) n++;
    if (this.computeCategorieEmploye(input) !== null) n++;
    return n;
  },
} as const;
