import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { MOTIFS_OQT, MotifOqt } from '../../core/models/annexe13-be.model';

/**
 * F-236 SF-236-02 — Helper partagé pour `Annexe13BeSectionComponent`
 * (F-IM-08-annexe13-be) — outil belge.
 *
 * F-236 SF-236-04 — gating workspaceCountry === 'BELGIQUE' appliqué dans
 * chaque `compute*` (early return null) + dans `computePrefillCount`
 * (early return 0). Pattern miroir de l'`ImmigrationWorkRightPrefillRules`.
 *
 * 4 champs : dateNotificationAnnexe13, delaiDepartImposeJours,
 * motifOqtCodeBe (enum), transfertImminentDetected (boolean).
 */

export const MOTIFS_OQT_WHITELIST = new Set<MotifOqt>(MOTIFS_OQT.map(m => m.code));

/** F-236 SF-236-04 : gating BE-only. */
function isBelgium(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'BELGIQUE';
}

export const Annexe13BePrefillRules = {
  MOTIFS_OQT_WHITELIST,

  computeDateNotificationAnnexe13(input: PrefillCountInput): string | null {
    if (!isBelgium(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const d = ai.dateNotificationAnnexe13;
    if (typeof d !== 'string' || d.length === 0) return null;
    return d;
  },

  computeDelaiDepartImposeJours(input: PrefillCountInput): number | null {
    if (!isBelgium(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.delaiDepartImposeJours;
    if (typeof v !== 'number' || !Number.isInteger(v) || v < 0) return null;
    return v;
  },

  computeMotifOqt(input: PrefillCountInput): MotifOqt | null {
    if (!isBelgium(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const m = ai.motifOqtCodeBe;
    if (!m || typeof m !== 'string' || !MOTIFS_OQT_WHITELIST.has(m as MotifOqt)) return null;
    return m as MotifOqt;
  },

  computeTransfertImminent(input: PrefillCountInput): boolean | null {
    if (!isBelgium(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    if (typeof ai.transfertImminentDetected !== 'boolean') return null;
    return ai.transfertImminentDetected;
  },

  computePrefillCount(input: PrefillCountInput): number {
    // F-236 SF-236-04 : gating BE-only — court-circuit explicite pour parité avec les compute*.
    if (!isBelgium(input)) return 0;
    let n = 0;
    if (this.computeDateNotificationAnnexe13(input) !== null) n++;
    if (this.computeDelaiDepartImposeJours(input) !== null) n++;
    if (this.computeMotifOqt(input) !== null) n++;
    if (this.computeTransfertImminent(input) !== null) n++;
    return n;
  },
} as const;
