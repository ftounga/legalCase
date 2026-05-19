import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { MotifPlacementJld } from '../../core/models/jld-retention.model';

/**
 * SF-208-05 / SF-246-17 — Helper partage pour {@link JldRetentionSectionComponent}
 * (F-IM-21-jld-retention-fr) — FR mono-pays.
 *
 * Champs pre-fill :
 *  - dateNotificationPlacement : derivee de `aiData.dateNotificationOqtf`
 *    (le placement en CRA suit typiquement la notification d'une OQTF —
 *    fallback `aiData.dateNotificationDecisionContestee`).
 *  - motifPlacement : si `aiData.motifOqtfCode` est present, on infere
 *    `EXECUTION_OQTF` (cas le plus frequent : placement pour executer une OQTF
 *    venant d'etre notifiee). Note : signal indirect — l'avocat peut corriger.
 *  - recoursForme : depuis `aiData.recoursFormeDetected` (SF-246-17).
 *    OUI → true, NON → false, INCONNU ou null → null (no-op gracieux).
 *
 * Champs NON pre-remplis :
 *  - dateRecours : acte a venir, saisi par l'avocat.
 *
 * Total : 3 champs pre-remplissables (sur 4 saisissables).
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

  /**
   * SF-246-17 : recours forméе detectable depuis `recoursFormeDetected`.
   * OUI → true ; NON → false ; INCONNU ou absent → null (no-op gracieux).
   * Note : signal OQTF générique — peut être utilisé pour JLD car
   * le placement en CRA suit toujours une OQTF.
   */
  computeRecoursForme(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const detected = ai.recoursFormeDetected;
    if (!detected || typeof detected !== 'object') return null;
    const reponse = typeof detected.reponse === 'string'
      ? detected.reponse.trim().toUpperCase()
      : null;
    if (reponse === 'OUI') return true;
    if (reponse === 'NON') return false;
    return null; // INCONNU ou absent → no-op
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateNotificationPlacement(input) !== null) n++;
    if (this.computeMotifPlacement(input) !== null) n++;
    // SF-246-17 : recours forme
    if (this.computeRecoursForme(input) !== null) n++;
    return n;
  },
} as const;
