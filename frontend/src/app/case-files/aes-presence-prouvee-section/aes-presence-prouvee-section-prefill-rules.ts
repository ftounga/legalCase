import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { PeriodePresentee } from '../../core/models/aes-presence-prouvee.model';

/**
 * SF-214-12 — Helper partagé pour {@link AesPresenceProuveeSectionComponent}
 * (F-IM-30-aes-presence-prouvee-fr) — FR mono-pays.
 *
 * Pré-fill (depuis {@link ImmigrationExtractedData}) :
 *  - `aesDateEntreeFrance` (ISO YYYY-MM-DD valide, non future) → une période
 *    initiale `{ debut: aesDateEntreeFrance, fin: aujourd'hui, typePiece: 'AUTRE' }`.
 *
 * Aucune autre donnée IA n'alimente une ligne : l'avocat ajoute les périodes
 * justifiées par pièce. Le compteur de pré-fill reflète le nombre de lignes
 * que `prefillFromAi()` poserait (0 ou 1).
 *
 * Total : 1 ligne pré-remplissable.
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

/** Date du jour au format ISO YYYY-MM-DD (UTC-safe via slice). */
export function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

function isValidPastOrTodayIso(value: unknown): value is string {
  if (typeof value !== 'string' || !ISO_DATE_RE.test(value)) return false;
  const ts = Date.parse(value);
  if (Number.isNaN(ts)) return false;
  // Refuse les dates futures (présence prouvée impossible dans le futur).
  return value <= todayIso();
}

export const AesPresenceProuveePrefillRules = {
  todayIso,

  /**
   * Construit la période initiale à partir de la date d'entrée en France.
   * Retourne `null` si la date est absente, mal formée ou future.
   */
  computeInitialPeriode(input: PrefillCountInput): PeriodePresentee | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const debut = ai.aesDateEntreeFrance;
    if (!isValidPastOrTodayIso(debut)) return null;
    return { debut, fin: todayIso(), typePiece: 'AUTRE' };
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    return this.computeInitialPeriode(input) !== null ? 1 : 0;
  },
} as const;
