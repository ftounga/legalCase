import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LIENS_FAMILIAUX, LienFamilial } from '../../core/models/belgian-40bis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * F-236 SF-236-02 — Helper partagé pour `Belgian40bisSectionComponent`
 * (F-IM-14-40bis-cohabitant-ue-be) — BE only.
 *
 * F-236 SF-236-04 : gating workspaceCountry === 'BELGIQUE' appliqué
 * dans `compute*` (early return null) + dans `computePrefillCount`
 * (early return 0). Pattern miroir de `ImmigrationWorkRightPrefillRules`.
 *
 * SF-246-20 : 1 champ réel branché :
 *   - lienFamilial (← aiData.be40bisLienFamilial — whitelist LienFamilial 40bis 5 valeurs)
 * Champs existants inchangés : dateDepotDemande, regroupantCitoyenUe.
 */

export const LIENS_FAMILIAUX_40BIS_WHITELIST = new Set<LienFamilial>(
  LIENS_FAMILIAUX.map((l) => l.code),
);

/** F-236 SF-236-04 : gating BE-only. */
function isBelgium(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'BELGIQUE';
}

export const Belgian40bisPrefillRules = {
  LIENS_FAMILIAUX_40BIS_WHITELIST,

  /**
   * SF-246-20 : lien familial art. 40bis — lit le champ typé be40bisLienFamilial.
   * Whitelist 5 valeurs : CONJOINT / PARTENAIRE_ENREGISTRE / DESCENDANT_MINEUR /
   * DESCENDANT_MAJEUR_CHARGE / ASCENDANT_CHARGE.
   */
  computeLienFamilial(input: PrefillCountInput): LienFamilial | null {
    if (!isBelgium(input)) return null;
    const ai = input.aiData as ImmigrationExtractedData | null | undefined;
    if (!ai) return null;
    const v = ai.be40bisLienFamilial;
    if (typeof v !== 'string' || !LIENS_FAMILIAUX_40BIS_WHITELIST.has(v as LienFamilial)) return null;
    return v as LienFamilial;
  },

  computeDateDepotDemande(input: PrefillCountInput): string | null {
    if (!isBelgium(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const d = ai.dateDepotProcedure;
    if (typeof d !== 'string' || d.length === 0) return null;
    return d;
  },

  /**
   * Best-effort : si l'IA détecte un client UE, on suppose le regroupant
   * UE aussi (l'avocat ajustera). Posé strictement si nationaliteUe est
   * un boolean.
   */
  computeRegroupantCitoyenUe(input: PrefillCountInput): boolean | null {
    if (!isBelgium(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    if (typeof ai.nationaliteUe !== 'boolean') return null;
    return ai.nationaliteUe;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isBelgium(input)) return 0;
    let n = 0;
    if (this.computeLienFamilial(input) !== null) n++;
    if (this.computeDateDepotDemande(input) !== null) n++;
    if (this.computeRegroupantCitoyenUe(input) !== null) n++;
    return n;
  },
} as const;
