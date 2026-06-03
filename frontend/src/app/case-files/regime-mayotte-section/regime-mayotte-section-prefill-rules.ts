import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { TypeTitreMayotte } from '../../core/models/regime-mayotte.model';

/**
 * SF-220-02 — Helper partagé pour {@link RegimeMayotteSectionComponent}
 * (F-IM-48-regime-mayotte-fr) — FR mono-pays.
 *
 * Champs pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - titreDelivreAMayotte : `aiData.mayotteTitreDelivreAMayotte` (booléen).
 *  - typeTitre : `aiData.mayotteTypeTitre` (whitelist 5 codes).
 *  - projetDeplacementMetropole : `aiData.mayotteProjetDeplacementMetropole` (booléen).
 *
 * Champ NON pré-rempli :
 *  - dateDelivrance : pas de pré-fill de date dans cet outil (saisie avocat optionnelle).
 *
 * Le flag `mayotteDetecte` est le pivot de VISIBILITÉ (CONTEXTUAL), pas un champ
 * saisissable du formulaire — il ne compte pas dans le pré-fill.
 *
 * Total : 3 champs pre-remplissables (sur 4 saisissables, dateDelivrance hors pré-fill).
 */

export const TYPE_TITRE_MAYOTTE_SET: ReadonlySet<TypeTitreMayotte> =
  new Set<TypeTitreMayotte>(['VPF', 'SALARIE', 'ETUDIANT', 'RESIDENT', 'AUTRE']);

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const RegimeMayottePrefillRules = {
  TYPE_TITRE_MAYOTTE_SET,

  computeTitreDelivreAMayotte(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.mayotteTitreDelivreAMayotte;
    if (typeof v !== 'boolean') return null;
    return v;
  },

  computeTypeTitre(input: PrefillCountInput): TypeTitreMayotte | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.mayotteTypeTitre;
    if (typeof v !== 'string') return null;
    const upper = v.trim().toUpperCase();
    if (!TYPE_TITRE_MAYOTTE_SET.has(upper as TypeTitreMayotte)) return null;
    return upper as TypeTitreMayotte;
  },

  computeProjetDeplacementMetropole(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.mayotteProjetDeplacementMetropole;
    if (typeof v !== 'boolean') return null;
    return v;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeTitreDelivreAMayotte(input) !== null) n++;
    if (this.computeTypeTitre(input) !== null) n++;
    if (this.computeProjetDeplacementMetropole(input) !== null) n++;
    return n;
  },
} as const;
