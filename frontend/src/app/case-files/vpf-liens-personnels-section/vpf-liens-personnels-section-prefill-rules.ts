import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { NiveauIntegration } from '../../core/models/vpf-liens-personnels.model';

/**
 * SF-214-06 — Helper partagé pour {@link VpfLiensPersonnelsSectionComponent}
 * (F-IM-27-vpf-liens-personnels-l42323-fr) — FR mono-pays.
 *
 * Champs pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - dureeResidenceFranceMois : `aiData.aesDureePresenceMois` (entier ≥ 0).
 *  - entreeEnFranceMineur     : `aiData.clientMineurDetecte === true`.
 *  - enfantsEnFrance          : `aiData.aesDureeScolaritePlusAncienEnfantAnnees > 0`.
 *  - niveauIntegration        : `aiData.vpfNiveauIntegration` (whitelist FORT/MOYEN/FAIBLE).
 *
 * Champs NON pré-remplis :
 *  - conjointEnFrance / parentsEnFrance / situationFamilialeAlEtranger /
 *    ancienneConvictionPenale : pas de signal IA fiable, saisis par l'avocat.
 *
 * Total : 4 champs pre-remplissables (sur 8 saisissables).
 */

export const NIVEAU_INTEGRATION_SET: ReadonlySet<NiveauIntegration> =
  new Set<NiveauIntegration>(['FORT', 'MOYEN', 'FAIBLE']);

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const VpfLiensPersonnelsPrefillRules = {
  NIVEAU_INTEGRATION_SET,

  computeDureeResidenceFranceMois(input: PrefillCountInput): number | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.aesDureePresenceMois;
    if (typeof v !== 'number' || !Number.isFinite(v) || v < 0) return null;
    return Math.trunc(v);
  },

  computeEntreeEnFranceMineur(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return ai.clientMineurDetecte === true ? true : null;
  },

  computeEnfantsEnFrance(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.aesDureeScolaritePlusAncienEnfantAnnees;
    if (typeof v !== 'number' || !Number.isFinite(v) || v <= 0) return null;
    return true;
  },

  computeNiveauIntegration(input: PrefillCountInput): NiveauIntegration | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.vpfNiveauIntegration;
    if (typeof v !== 'string') return null;
    const upper = v.trim().toUpperCase();
    if (!NIVEAU_INTEGRATION_SET.has(upper as NiveauIntegration)) return null;
    return upper as NiveauIntegration;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDureeResidenceFranceMois(input) !== null) n++;
    if (this.computeEntreeEnFranceMineur(input) !== null) n++;
    if (this.computeEnfantsEnFrance(input) !== null) n++;
    if (this.computeNiveauIntegration(input) !== null) n++;
    return n;
  },
} as const;
