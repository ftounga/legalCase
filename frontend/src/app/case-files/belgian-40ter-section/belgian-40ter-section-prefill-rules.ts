import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { LIENS_FAMILIAUX, LienFamilial } from '../../core/models/belgian-40ter.model';

/**
 * F-236 SF-236-02 — Helper partagé pour `Belgian40terSectionComponent`
 * (F-IM-14-40ter-familial-belge-be) — BE only.
 *
 * F-236 SF-236-04 : gating workspaceCountry === 'BELGIQUE' appliqué
 * dans `compute*` (early return null) + dans `computePrefillCount`
 * (early return 0). Pattern miroir de `ImmigrationWorkRightPrefillRules`.
 *
 * SF-246-20 : 3 champs réels branchés :
 *   - lienFamilial (← aiData.be40terLienFamilial — whitelist LienFamilial 40ter 5 valeurs)
 *   - revenusMensuelsNets (← aiData.be40terRevenusMensuelsNets — > 0 et ≤ 30 000)
 *   - dateDepotDemande (← aiData.dateDepotProcedure — champ typé, alias supprimé)
 * Champ existant inchangé : regroupantBelge.
 */

const MAX_REVENUS_MENSUELS = 30_000;

export const LIENS_FAMILIAUX_WHITELIST = new Set<LienFamilial>(
  LIENS_FAMILIAUX.map((l) => l.code),
);

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

/** F-236 SF-236-04 : gating BE-only. */
function isBelgium(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'BELGIQUE';
}

export const Belgian40terPrefillRules = {
  LIENS_FAMILIAUX_WHITELIST,

  /**
   * SF-246-20 : lien familial art. 40ter — lit le champ typé be40terLienFamilial.
   * Whitelist 40ter (5 valeurs DISTINCTES de 40bis) :
   *   CONJOINT / PARTENAIRE_LEGAL_ENREGISTRE / DESCENDANT_MINEUR /
   *   DESCENDANT_MAJEUR_CHARGE / ASCENDANT_CHARGE_HANDICAP.
   * PARTENAIRE_ENREGISTRE et ASCENDANT_CHARGE (40bis) sont REJETÉS ici.
   */
  computeLienFamilial(input: PrefillCountInput): LienFamilial | null {
    if (!isBelgium(input)) return null;
    const ai = input.aiData as ImmigrationExtractedData | null | undefined;
    if (!ai) return null;
    const v = ai.be40terLienFamilial;
    if (typeof v !== 'string' || !LIENS_FAMILIAUX_WHITELIST.has(v as LienFamilial)) return null;
    return v as LienFamilial;
  },

  computeRegroupantBelge(input: PrefillCountInput): boolean | null {
    if (!isBelgium(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    if (typeof ai.regroupantBelge !== 'boolean') return null;
    return ai.regroupantBelge;
  },

  /**
   * SF-246-20 : revenus mensuels nets — lit be40terRevenusMensuelsNets.
   * Valide si > 0 et ≤ 30 000 (plafond MAX_BE_REVENUS_MENSUELS_NETS backend).
   * 0, négatif, NaN, > 30 000 → null.
   */
  computeRevenusMensuelsNets(input: PrefillCountInput): number | null {
    if (!isBelgium(input)) return null;
    const ai = input.aiData as ImmigrationExtractedData | null | undefined;
    if (!ai) return null;
    const v = ai.be40terRevenusMensuelsNets;
    if (typeof v !== 'number' || isNaN(v) || v <= 0 || v > MAX_REVENUS_MENSUELS) return null;
    return v;
  },

  /**
   * SF-246-20 : date dépôt demande — lit dateDepotProcedure (champ typé canonique).
   * Alias aspirationnels dateDepotDemande supprimés.
   * Rejeté si futur.
   */
  computeDateDepotDemande(input: PrefillCountInput): string | null {
    if (!isBelgium(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const d = ai.dateDepotProcedure;
    if (typeof d !== 'string' || d.length === 0) return null;
    if (d > todayIso()) return null;
    return d;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isBelgium(input)) return 0;
    let n = 0;
    if (this.computeLienFamilial(input) !== null) n++;
    if (this.computeRegroupantBelge(input) !== null) n++;
    if (this.computeRevenusMensuelsNets(input) !== null) n++;
    if (this.computeDateDepotDemande(input) !== null) n++;
    return n;
  },
} as const;
