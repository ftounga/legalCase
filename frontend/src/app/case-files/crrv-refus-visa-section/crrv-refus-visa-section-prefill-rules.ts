import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { TypeVisaCrrv } from '../../core/models/crrv-refus-visa.model';

/**
 * SF-208-07 — Helper partage pour {@link CrrvRefusVisaSectionComponent}
 * (F-IM-23-crrv-refus-visa-fr) — FR mono-pays.
 *
 * Champs pre-fill :
 *  - dateNotificationRefus : `aiData.dateNotificationDecisionContestee` ISO non future.
 *
 * Champs NON pre-remplis (signal IA absent dans ImmigrationExtractedData V1) :
 *  - typeVisa : pas d'extraction Sonnet dediee aux types de visa actuellement.
 *  - motifRefus : champ libre, saisi par l'avocat.
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export const TYPES_VISA_CRRV_SET: ReadonlySet<TypeVisaCrrv> =
  new Set<TypeVisaCrrv>([
    'COURT_SEJOUR',
    'LONG_SEJOUR',
    'REGROUPEMENT_FAMILIAL',
    'ETUDIANT',
    'AUTRE',
  ]);

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const CrrvRefusVisaPrefillRules = {
  ISO_DATE_RE,
  TYPES_VISA_CRRV_SET,

  computeDateNotificationRefus(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const candidate =
      typeof ai.dateNotificationDecisionContestee === 'string'
        ? ai.dateNotificationDecisionContestee
        : null;
    if (!candidate || !ISO_DATE_RE.test(candidate)) return null;
    if (candidate > todayIso()) return null;
    return candidate;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateNotificationRefus(input) !== null) n++;
    return n;
  },
} as const;
