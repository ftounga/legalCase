/**
 * F-236 SF-236-02 — Helper partagé `DivorceChecklistPrefillRules` (module pur).
 *
 * <p>Note divergence connue (laissée pour SF-236-03) : le runtime marque
 * jusqu'à 2 étapes "signature convention" (FR + BE) si elles sont toutes deux
 * présentes dans la checklist générée backend, mais `computePrefillCount`
 * retourne 1 (1 seule étape attendue par pays). La divergence sera corrigée
 * en SF-236-03 ; ici le helper consolide la **duplication runtime/static**
 * sans changer le compteur.
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

/**
 * Codes d'étapes "signature convention" que le pré-fill IA marque FAIT
 * (FR : signature CM ; BE : rédaction convention).
 */
export const SIGNATURE_STEP_CODES: readonly string[] = [
  'FR_SIGNATURE_CONVENTION',
  'BE_REDACTION_CONVENTION',
];

export interface DivorceChecklistPrefillInput {
  aiData?: FamilleExtractedData | null;
  // Champs ignorés (contrat unifié).
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

/** Format YYYY-MM-DD validé strictement. */
export function isIsoDate(value: unknown): value is string {
  return typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value);
}

/**
 * Détermine si `dateAcceptationPV` est exploitable pour le pré-fill.
 * Retourne la date ISO si oui, null sinon.
 */
export function computeDateAcceptationPV(input: DivorceChecklistPrefillInput): string | null {
  const ai = input.aiData;
  if (!ai) return null;
  return isIsoDate(ai.dateAcceptationPV) ? ai.dateAcceptationPV : null;
}

/**
 * Compteur de champs pré-remplis. Toujours 0 ou 1 — la checklist du pays
 * courant contient au plus une étape "signature convention".
 *
 * <p>Le runtime peut marquer plusieurs étapes (cas FR + BE simultanés —
 * divergence connue traitée par SF-236-03). Le badge utilisateur reste à 1
 * pour ne pas tromper sur le nombre logique de champs pré-remplis.
 */
export function computePrefillCount(input: DivorceChecklistPrefillInput): number {
  return computeDateAcceptationPV(input) !== null ? 1 : 0;
}

export const DivorceChecklistPrefillRules = {
  SIGNATURE_STEP_CODES,
  isIsoDate,
  computeDateAcceptationPV,
  computePrefillCount,
};
