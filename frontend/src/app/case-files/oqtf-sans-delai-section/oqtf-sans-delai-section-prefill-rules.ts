import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { MOTIFS_SANS_DELAI, MotifSansDelai } from '../../core/models/oqtf-sans-delai.model';

/**
 * F-236 SF-236-02 — Helper partagé pour `OqtfSansDelaiSectionComponent`
 * (F-IM-08-oqtf-sans-delai-fr) — FR mono-pays.
 *
 * 4 champs : dateHeureNotificationOqtf (datetime-local), motifSansDelai
 * (enum subset MotifSansDelai), placementCra (boolean), recoursForme
 * (boolean via DetectedAnswer OUI/NON).
 */

export const ALLOWED_MOTIFS_SANS_DELAI: ReadonlySet<string> = new Set<string>(
  MOTIFS_SANS_DELAI.map((m) => m.code),
);

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

/** Convertit un ISO partiel (YYYY-MM-DDTHH:mm[:ss]) en chaîne datetime-local. */
export function normalizeDatetimeLocalInput(iso: string | null | undefined): string | null {
  if (!iso) return null;
  const m = iso.match(/^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2})(:\d{2})?$/);
  if (!m) return null;
  return m[1];
}

export const OqtfSansDelaiPrefillRules = {
  ALLOWED_MOTIFS_SANS_DELAI,
  normalizeDatetimeLocalInput,

  computeDateHeureNotificationOqtf(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return normalizeDatetimeLocalInput(ai.dateHeureNotificationOqtfSansDelai);
  },

  computeMotifSansDelai(input: PrefillCountInput): MotifSansDelai | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const raw = ai.motifOqtfCode;
    if (!raw || !ALLOWED_MOTIFS_SANS_DELAI.has(raw)) return null;
    return raw as MotifSansDelai;
  },

  computePlacementCra(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    if (ai.placementCraDetected === true) return true;
    if (ai.placementCraDetected === false) return false;
    return null;
  },

  computeRecoursForme(input: PrefillCountInput): boolean | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const r = ai.recoursFormeDetected?.reponse;
    if (r === 'OUI') return true;
    if (r === 'NON') return false;
    return null;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateHeureNotificationOqtf(input) !== null) n++;
    if (this.computeMotifSansDelai(input) !== null) n++;
    if (this.computePlacementCra(input) !== null) n++;
    if (this.computeRecoursForme(input) !== null) n++;
    return n;
  },
} as const;
