import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * F-236 SF-236-02 — Helper partagé pour `Belgian9bisSectionComponent`
 * (F-IM-14-9bis-humanitaire-be) — BE only.
 *
 * F-236 SF-236-04 : gating workspaceCountry === 'BELGIQUE' appliqué
 * dans `compute*` (early return null) + dans `computePrefillCount`
 * (early return 0). Pattern miroir de `ImmigrationWorkRightPrefillRules`.
 *
 * SF-246-20 : 2 nouveaux champs réels branchés :
 *   - dateEntreeBelgique (← aiData.be9bisDateEntreeBelgique — ISO strict, non-future)
 *   - dureePresenceMois  (← aiData.be9bisDureePresenceMois — calculé backend)
 * Champ existant inchangé : dateDepotDemande.
 */

/** ISO date strict YYYY-MM-DD. */
const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

/** F-236 SF-236-04 : gating BE-only. */
function isBelgium(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'BELGIQUE';
}

export const Belgian9bisPrefillRules = {
  /**
   * SF-246-20 : date d'entrée en Belgique — lit le champ typé be9bisDateEntreeBelgique.
   * Validation ISO strict ; non-future vérifiée backend.
   */
  computeDateEntreeBelgique(input: PrefillCountInput): string | null {
    if (!isBelgium(input)) return null;
    const ai = input.aiData as ImmigrationExtractedData | null | undefined;
    if (!ai) return null;
    const v = ai.be9bisDateEntreeBelgique;
    if (typeof v !== 'string' || !ISO_DATE_RE.test(v)) return null;
    return v;
  },

  /**
   * SF-246-20 : durée de présence en mois — lit le champ typé be9bisDureePresenceMois.
   * Calculé backend depuis be9bisDateEntreeBelgique. 0 est une valeur valide.
   */
  computeDureePresenceMois(input: PrefillCountInput): number | null {
    if (!isBelgium(input)) return null;
    const ai = input.aiData as ImmigrationExtractedData | null | undefined;
    if (!ai) return null;
    const v = ai.be9bisDureePresenceMois;
    if (typeof v !== 'number' || isNaN(v)) return null;
    return v;
  },

  computeDateDepotDemande(input: PrefillCountInput): string | null {
    if (!isBelgium(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const d = ai.dateDepotProcedure;
    if (typeof d !== 'string' || d.length === 0) return null;
    return d;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isBelgium(input)) return 0;
    let n = 0;
    if (this.computeDateEntreeBelgique(input) !== null) n++;
    if (this.computeDureePresenceMois(input) !== null) n++;
    if (this.computeDateDepotDemande(input) !== null) n++;
    return n;
  },
} as const;
