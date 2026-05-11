import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { MotifTransfertDublin } from '../../core/models/dublin-recours.model';

/**
 * SF-208-06 — Helper partage pour {@link DublinRecoursSectionComponent}
 * (F-IM-22-dublin-recours-fr) — FR mono-pays.
 *
 * Champs pre-fill :
 *  - dateNotificationDecisionTransfert : `aiData.dateNotificationDecisionContestee`
 *    (source generique des decisions immigration contestees) ou
 *    `aiData.dateNotificationOqtf` (fallback secondaire). ISO non future requis.
 *
 * Champs NON pre-remplis (aucun signal IA fiable dans ImmigrationExtractedData
 * actuel — etatMembreResponsable et motifTransfert restent saisis par l'avocat).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export const MOTIFS_TRANSFERT_DUBLIN_SET: ReadonlySet<MotifTransfertDublin> =
  new Set<MotifTransfertDublin>([
    'DEMANDE_ASILE_AUTRE_ETAT',
    'VISA_DELIVRE_AUTRE_ETAT',
    'ENTREE_IRREGULIERE_AUTRE_ETAT',
    'MEMBRE_FAMILLE_AUTRE_ETAT',
    'AUTRE',
  ]);

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const DublinRecoursPrefillRules = {
  ISO_DATE_RE,
  MOTIFS_TRANSFERT_DUBLIN_SET,

  computeDateNotificationDecisionTransfert(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const candidate =
      (typeof ai.dateNotificationDecisionContestee === 'string'
        ? ai.dateNotificationDecisionContestee
        : null) ??
      (typeof ai.dateNotificationOqtf === 'string' ? ai.dateNotificationOqtf : null);
    if (!candidate || !ISO_DATE_RE.test(candidate)) return null;
    if (candidate > todayIso()) return null;
    return candidate;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateNotificationDecisionTransfert(input) !== null) n++;
    return n;
  },
} as const;
