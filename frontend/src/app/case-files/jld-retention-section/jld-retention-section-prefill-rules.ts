import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { MotifPlacementJld } from '../../core/models/jld-retention.model';

/**
 * SF-208-05 — Helper partage pour {@link JldRetentionSectionComponent}
 * (F-IM-21-jld-retention-fr) — FR mono-pays.
 *
 * Champs pre-fill :
 *  - dateNotificationPlacement : derivee de `aiData.dateNotificationOqtf`
 *    (le placement en CRA suit typiquement la notification d'une OQTF —
 *    fallback `aiData.dateNotificationDecisionContestee`).
 *  - motifPlacement : si `aiData.motifOqtfCode` est present, on infere
 *    `EXECUTION_OQTF` (cas le plus frequent : placement pour executer une OQTF
 *    venant d'etre notifiee). Note : signal indirect — l'avocat peut corriger.
 *
 * Champs NON pre-remplis (champs sans signal IA fiable) :
 *  - recoursForme / dateRecours (toujours geres manuellement par l'avocat).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export const MOTIFS_PLACEMENT_JLD_SET: ReadonlySet<MotifPlacementJld> =
  new Set<MotifPlacementJld>([
    'EXECUTION_OQTF',
    'ITF',
    'INTERDICTION_TERRITOIRE',
    'DUBLIN_TRANSFERT',
    'AUTRE',
  ]);

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const JldRetentionPrefillRules = {
  ISO_DATE_RE,
  MOTIFS_PLACEMENT_JLD_SET,

  computeDateNotificationPlacement(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    // Source canonique : dateNotificationOqtf (le placement suit l'OQTF).
    // Fallback : dateNotificationDecisionContestee.
    const candidate =
      (typeof ai.dateNotificationOqtf === 'string' ? ai.dateNotificationOqtf : null) ??
      (typeof ai.dateNotificationDecisionContestee === 'string'
        ? ai.dateNotificationDecisionContestee
        : null);
    if (!candidate || !ISO_DATE_RE.test(candidate)) return null;
    if (candidate > todayIso()) return null;
    return candidate;
  },

  computeMotifPlacement(input: PrefillCountInput): MotifPlacementJld | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    // Inference indirecte : si une OQTF est detectee (motifOqtfCode present),
    // le motif de placement le plus probable est EXECUTION_OQTF.
    if (ai.motifOqtfCode && typeof ai.motifOqtfCode === 'string') {
      return 'EXECUTION_OQTF';
    }
    return null;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateNotificationPlacement(input) !== null) n++;
    if (this.computeMotifPlacement(input) !== null) n++;
    return n;
  },
} as const;
