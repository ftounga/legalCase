import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { TypeVisaCrrv } from '../../core/models/crrv-refus-visa.model';

/**
 * SF-208-07 / SF-246-17 — Helper partage pour {@link CrrvRefusVisaSectionComponent}
 * (F-IM-23-crrv-refus-visa-fr) — FR mono-pays.
 *
 * Champs pre-fill :
 *  - dateNotificationRefus : `aiData.dateNotificationDecisionContestee` ISO non future.
 *  - typeVisa : `aiData.crrvTypeVisa` (code enum normalise, SF-246-17).
 *  - motifRefus : `aiData.crrvMotifRefus` (texte libre, SF-246-17).
 *
 * Total : 3 champs pre-remplissables (sur 5 saisissables — recoursForme + dateRecours
 * restent saisis par l'avocat car ce sont des actes a venir).
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

  /** SF-246-17 : type de visa refusé — code enum normalisé (whitelist 5 codes). */
  computeTypeVisa(input: PrefillCountInput): TypeVisaCrrv | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.crrvTypeVisa;
    if (typeof v !== 'string') return null;
    const upper = v.trim().toUpperCase() as TypeVisaCrrv;
    return TYPES_VISA_CRRV_SET.has(upper) ? upper : null;
  },

  /** SF-246-17 : motif de refus de visa — texte libre non vide (≤ 500 car.). */
  computeMotifRefus(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.crrvMotifRefus;
    if (typeof v !== 'string') return null;
    const trimmed = v.trim();
    return trimmed.length > 0 ? trimmed : null;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateNotificationRefus(input) !== null) n++;
    // SF-246-17 : 2 nouveaux champs
    if (this.computeTypeVisa(input) !== null) n++;
    if (this.computeMotifRefus(input) !== null) n++;
    return n;
  },
} as const;
