import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { MotifInterdictionEntree } from '../../core/models/annexe13quinquies-be.model';

/**
 * SF-215-18 — Helper partagé pour {@link Annexe13quinquiesBeSectionComponent}
 * (F-IM-33-annexe13quinquies-ie-be) — BE mono-pays.
 *
 * L'annexe 13quinquies est un OQT assorti d'une interdiction d'entrée Schengen
 * (Loi 15/12/1980, art. 74/11). BELGIQUE uniquement.
 *
 * Champs pré-fill RÉELS (depuis {@link ImmigrationExtractedData}, miroir backend) :
 *  - dateNotificationAnnexe  : `aiData.interdictionEntreeDateNotification` (ISO yyyy-MM-dd)
 *  - motifInterdictionEntree : `aiData.interdictionEntreeMotif`
 *      (whitelist SEJOUR_IRREGULIER / MENACE_ORDRE_PUBLIC /
 *       RAISONS_SECURITE_NATIONALE / ATTEINTE_INTERET_UE / DECISION_JUDICIAIRE)
 *
 * Total : 2 champs pré-remplissables. Les champs `precedentSejour` (checkbox),
 * `recoursForme` (checkbox) et `dateRecours` (date conditionnelle) sont
 * ASPIRATIONNELS — non extraits par l'IA, saisie avocat — et ne comptent JAMAIS
 * dans le prefill count.
 *
 * Gate BELGIQUE : `workspaceCountry === 'BELGIQUE'` (strict). Sur workspace FR
 * tout retourne null (le composant affiche une bannière info dans ce cas).
 */

export const ANNEXE13QUINQUIES_MOTIF_SET: ReadonlySet<MotifInterdictionEntree> =
  new Set<MotifInterdictionEntree>([
    'SEJOUR_IRREGULIER',
    'MENACE_ORDRE_PUBLIC',
    'RAISONS_SECURITE_NATIONALE',
    'ATTEINTE_INTERET_UE',
    'DECISION_JUDICIAIRE',
  ]);

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

export const Annexe13quinquiesBePrefillRules = {
  ANNEXE13QUINQUIES_MOTIF_SET,

  computeDateNotificationAnnexe(input: PrefillCountInput): string | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return normalizeIsoDate(ai.interdictionEntreeDateNotification);
  },

  computeMotifInterdictionEntree(input: PrefillCountInput): MotifInterdictionEntree | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.interdictionEntreeMotif;
    if (typeof v !== 'string') return null;
    const upper = v.trim().toUpperCase();
    if (!ANNEXE13QUINQUIES_MOTIF_SET.has(upper as MotifInterdictionEntree)) return null;
    return upper as MotifInterdictionEntree;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isBelgique(input)) return 0;
    let n = 0;
    if (this.computeDateNotificationAnnexe(input) !== null) n++;
    if (this.computeMotifInterdictionEntree(input) !== null) n++;
    return n;
  },
} as const;
