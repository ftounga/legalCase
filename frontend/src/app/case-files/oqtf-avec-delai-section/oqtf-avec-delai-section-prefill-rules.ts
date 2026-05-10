import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { MotifOqtf } from '../../core/models/oqtf-avec-delai.model';

/**
 * F-236 SF-236-02 — Helper partagé pour `OqtfAvecDelaiSectionComponent`
 * (F-IM-08-oqtf-avec-delai-fr) — FR mono-pays.
 *
 * 3 champs : dateNotificationOqtf (ISO non future), motifOqtf (enum),
 * recoursForme (boolean dérivé de DetectedAnswer OUI/NON).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export const MOTIFS_OQTF_SET: ReadonlySet<MotifOqtf> = new Set<MotifOqtf>([
  'REFUS_TITRE', 'EXPIRATION_TITRE', 'SEJOUR_IRREGULIER', 'RETRAIT_TITRE', 'AUTRE',
]);

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const OqtfAvecDelaiPrefillRules = {
  ISO_DATE_RE,
  MOTIFS_OQTF_SET,

  computeDateNotificationOqtf(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const date = ai.dateNotificationOqtf;
    if (typeof date !== 'string' || !ISO_DATE_RE.test(date)) return null;
    if (date > todayIso()) return null;
    return date;
  },

  computeMotifOqtf(input: PrefillCountInput): MotifOqtf | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const code = ai.motifOqtfCode;
    if (!code || !MOTIFS_OQTF_SET.has(code as MotifOqtf)) return null;
    return code as MotifOqtf;
  },

  /**
   * recoursForme : posé uniquement si DetectedAnswer.reponse est OUI ou NON
   * (INCONNU = pas de signal). Boolean : true si OUI, false si NON.
   */
  computeRecoursForme(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const detected = ai.recoursFormeDetected;
    if (!detected) return null;
    if (detected.reponse === 'OUI') return true;
    if (detected.reponse === 'NON') return false;
    return null;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateNotificationOqtf(input) !== null) n++;
    if (this.computeMotifOqtf(input) !== null) n++;
    if (this.computeRecoursForme(input) !== null) n++;
    return n;
  },
} as const;
