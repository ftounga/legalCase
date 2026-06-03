import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { DetentionBaseLegale } from '../../core/models/detention-centre-ferme-be.model';

/**
 * SF-221-04 — Helper partagé pour {@link DetentionCentreFermeBeSectionComponent}
 * (F-IM-56-detention-centre-ferme-be) — BE mono-pays.
 *
 * Champs pré-fill RÉELS (depuis {@link ImmigrationExtractedData}, miroir backend) :
 *  - dateDebutDetention                  : `aiData.detentionDateDebut` (ISO yyyy-MM-dd)
 *  - baseLegaleDetention                 : `aiData.detentionBaseLegale` (whitelist 5 valeurs)
 *  - dateNotificationDecisionDetention   : `aiData.detentionDateNotification` (ISO yyyy-MM-dd)
 *
 * Total : 3 champs pré-remplissables. Les champs `prolongationNotifiee`,
 * `dateProlongation` et `requeteMiseEnLiberteDeposee` sont ASPIRATIONNELS — actions
 * procédurales non factualisées de façon fiable par l'IA (saisie / contrôle avocat
 * F-246) — et ne comptent JAMAIS dans le prefill count.
 *
 * Gate BELGIQUE : `workspaceCountry === 'BELGIQUE'` (strict). Sur workspace FR
 * tout retourne null (le composant affiche une bannière info dans ce cas).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;
const BASES_LEGALES: readonly DetentionBaseLegale[] = ['ART_7', 'ART_27', 'ART_29', 'ART_74_5', 'AUTRE'];

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

function normalizeBaseLegale(v: unknown): DetentionBaseLegale | null {
  if (typeof v !== 'string') return null;
  return BASES_LEGALES.includes(v as DetentionBaseLegale) ? (v as DetentionBaseLegale) : null;
}

export const DetentionCentreFermeBePrefillRules = {
  computeDateDebut(input: PrefillCountInput): string | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return normalizeIsoDate(ai.detentionDateDebut);
  },

  computeBaseLegale(input: PrefillCountInput): DetentionBaseLegale | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return normalizeBaseLegale(ai.detentionBaseLegale);
  },

  computeDateNotification(input: PrefillCountInput): string | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return normalizeIsoDate(ai.detentionDateNotification);
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isBelgique(input)) return 0;
    let n = 0;
    if (this.computeDateDebut(input) !== null) n++;
    if (this.computeBaseLegale(input) !== null) n++;
    if (this.computeDateNotification(input) !== null) n++;
    return n;
  },
} as const;
