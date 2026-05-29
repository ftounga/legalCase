import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-214-40 — Helper partagé pour {@link UeEeeSuisseSejourSectionComponent}
 * (F-IM-44-ue-eee-suisse-sejour-fr) — FRANCE mono-pays.
 *
 * Champs pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - nationalite : `aiData.nationalite` (texte non vide) — nationalité du
 *    ressortissant, identifiée depuis les pièces d'identité du dossier.
 *  - estCitoyenUE : `aiData.nationaliteUe` (booléen) — l'IA détermine si la
 *    nationalité relève de l'UE/EEE/Suisse. Pré-rempli uniquement si la valeur
 *    booléenne est présente (true ou false).
 *  - dureeSejourMois : `aiData.aesDureePresenceMois` (entier ≥ 0) — durée de
 *    présence en France calculée par le backend.
 *
 * Champs NON pré-remplis :
 *  - membreFamilleNonUE : appréciation laissée à l'avocat (lien familial +
 *    nationalité du demandeur) — non factualisable de façon fiable.
 *  - activiteProfessionnelle : qualification laissée à l'avocat (statut au
 *    regard du droit de séjour : salarié, indépendant, étudiant…).
 *
 * Total : 3 champs pre-remplissables (sur 5 saisissables).
 */

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const UeEeeSuisseSejourPrefillRules = {

  computeNationalite(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = (ai as { nationalite?: unknown }).nationalite;
    if (typeof v !== 'string') return null;
    const trimmed = v.trim();
    if (trimmed.length === 0) return null;
    return trimmed;
  },

  computeEstCitoyenUE(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = (ai as { nationaliteUe?: unknown }).nationaliteUe;
    if (typeof v !== 'boolean') return null;
    return v;
  },

  computeDureeSejourMois(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = (ai as { aesDureePresenceMois?: unknown }).aesDureePresenceMois;
    if (typeof v !== 'number' || !Number.isFinite(v) || v < 0) return null;
    return Math.trunc(v);
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeNationalite(input) !== null) n++;
    if (this.computeEstCitoyenUE(input) !== null) n++;
    if (this.computeDureeSejourMois(input) !== null) n++;
    return n;
  },
} as const;
