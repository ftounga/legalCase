import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-221-05 — Helper partagé pour {@link CceSuspensionBeSectionComponent}
 * (F-IM-57-cce-suspension-be) — BE mono-pays.
 *
 * Champs pré-fill RÉELS (depuis {@link ImmigrationExtractedData}, miroir backend) :
 *  - dateNotificationDecision                   : `aiData.cceSuspensionDateNotification` (ISO yyyy-MM-dd)
 *  - urgenceInvocable                           : `aiData.cceSuspensionUrgence` (boolean)
 *  - risquePrejudiceGraveDifficilementReparable : `aiData.cceSuspensionPrejudiceGrave` (boolean)
 *
 * Total : 3 champs pré-remplissables. Les champs `recoursAnnulationIntroduit` et
 * `mesureEloignementImminente` sont ASPIRATIONNELS — appréciations procédurales non
 * factualisées de façon fiable par l'IA (saisie / contrôle avocat F-246) — et ne
 * comptent JAMAIS dans le prefill count.
 *
 * Gate BELGIQUE : `workspaceCountry === 'BELGIQUE'` (strict). Sur workspace FR
 * tout retourne null (le composant affiche une bannière info dans ce cas).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function isBelgique(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'BELGIQUE';
}

function normalizeIsoDate(v: unknown): string | null {
  if (typeof v !== 'string') return null;
  const trimmed = v.trim();
  if (!ISO_DATE_RE.test(trimmed)) return null;
  // Validité calendaire stricte (ex. rejette 2026-02-30).
  const d = new Date(`${trimmed}T00:00:00Z`);
  if (isNaN(d.getTime())) return null;
  const iso = d.toISOString().slice(0, 10);
  return iso === trimmed ? trimmed : null;
}

function normalizeBoolean(v: unknown): boolean | null {
  return typeof v === 'boolean' ? v : null;
}

export const CceSuspensionBePrefillRules = {
  computeDateNotification(input: PrefillCountInput): string | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return normalizeIsoDate(ai.cceSuspensionDateNotification);
  },

  computeUrgence(input: PrefillCountInput): boolean | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return normalizeBoolean(ai.cceSuspensionUrgence);
  },

  computePrejudiceGrave(input: PrefillCountInput): boolean | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return normalizeBoolean(ai.cceSuspensionPrejudiceGrave);
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isBelgique(input)) return 0;
    let n = 0;
    if (this.computeDateNotification(input) !== null) n++;
    if (this.computeUrgence(input) !== null) n++;
    if (this.computePrejudiceGrave(input) !== null) n++;
    return n;
  },
} as const;
