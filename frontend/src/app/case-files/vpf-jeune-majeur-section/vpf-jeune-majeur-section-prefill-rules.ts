import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-220-03 — Helper partagé pour {@link VpfJeuneMajeurSectionComponent}
 * (F-IM-49-vpf-jeune-majeur-l42322-fr) — FR mono-pays.
 *
 * Champs pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - age : `aiData.jeuneMajeurAge` (entier ≥ 0).
 *  - entreMineur : `aiData.jeuneMajeurEntreMineur` (booléen).
 *  - priseEnChargeAse : `aiData.jeuneMajeurPriseEnChargeAse` (booléen).
 *  - scolariseOuFormation : `aiData.jeuneMajeurScolarise` (booléen).
 *
 * Champs NON pré-remplis (non extraits par le pipeline IA — non factualisables
 * de façon fiable depuis les pièces, saisie avocat) : ageEntreeAse,
 * dateEntreeFrance, dateDebutPriseEnCharge, ancienneteMoisPriseEnCharge,
 * caractereReelEtSerieuxFormation, avisStructureFavorable, absenceLienFamillePays.
 *
 * Le flag `jeuneMajeurExMnaDetecte` est le pivot de VISIBILITÉ (CONTEXTUAL),
 * pas un champ saisissable du formulaire — il ne compte pas dans le pré-fill.
 *
 * Total : 4 champs pre-remplissables.
 */

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const VpfJeuneMajeurPrefillRules = {

  computeAge(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.jeuneMajeurAge;
    if (typeof v !== 'number' || !Number.isFinite(v) || v < 0) return null;
    return v;
  },

  computeEntreMineur(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.jeuneMajeurEntreMineur;
    if (typeof v !== 'boolean') return null;
    return v;
  },

  computePriseEnChargeAse(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.jeuneMajeurPriseEnChargeAse;
    if (typeof v !== 'boolean') return null;
    return v;
  },

  computeScolariseOuFormation(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.jeuneMajeurScolarise;
    if (typeof v !== 'boolean') return null;
    return v;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeAge(input) !== null) n++;
    if (this.computeEntreMineur(input) !== null) n++;
    if (this.computePriseEnChargeAse(input) !== null) n++;
    if (this.computeScolariseOuFormation(input) !== null) n++;
    return n;
  },
} as const;
