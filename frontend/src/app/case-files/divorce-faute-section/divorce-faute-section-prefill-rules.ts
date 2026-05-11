/**
 * F-237 SF-237-02 — Helper partagé `DivorceFautePrefillRules` (module pur).
 *
 * Extrait la logique de pré-fill IA du composant `DivorceFauteSectionComponent`
 * (F-FA-09, FR uniquement, art. 242 Cciv) selon le contrat F-236 SF-236-01.
 *
 * 5 champs pré-fillables :
 *   1. dureeMariageAnnees (number > 0)
 *   2. revenusAnnuelsDemandeurEur (number > 0)
 *   3. revenusAnnuelsDefendeurEur (number > 0)
 *   4. dateDepotAssignation (string non vide — pas de stricte ISO ici, on
 *      reproduit la garde initiale `length > 0` du runtime)
 *   5. fautesDetectees (array de codes mappés dans `VALID_FAUTE_CODES`)
 *
 * Le module est pur (aucun Angular import).
 *
 * F-236 SF-236-03 : `fautesDetectees` est actuellement déclaré sur
 * `TravailExtractedData` côté modèle backend (anomalie connue — devra migrer
 * vers `FamilleExtractedData`). Cast permissif utilisé jusqu'au fix.
 */
import { FauteCode } from '../../core/models/divorce-faute.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

export const VALID_FAUTE_CODES: ReadonlySet<string> = new Set<FauteCode>([
  'ADULTERE',
  'VIOLENCES',
  'ABANDON',
  'OUTRAGES',
  'DEVOIR_ASSISTANCE',
  'DEVOIR_FIDELITE',
  'DEVOIR_COMMUNAUTE_VIE',
  'AUTRE',
]);

export interface DivorceFautePrefillInput {
  aiData?: FamilleExtractedData | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computeDureeMariage(input: DivorceFautePrefillInput): number | null {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const v = (input.aiData as any)?.dureeMariageAnnees;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computeRevenusDemandeur(input: DivorceFautePrefillInput): number | null {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const v = (input.aiData as any)?.revenusAnnuelsDemandeurEur;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computeRevenusDefendeur(input: DivorceFautePrefillInput): number | null {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const v = (input.aiData as any)?.revenusAnnuelsDefendeurEur;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computeDateDepotAssignation(input: DivorceFautePrefillInput): string | null {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const v = (input.aiData as any)?.dateDepotAssignation;
  return typeof v === 'string' && v.length > 0 ? v : null;
}

/**
 * Fautes détectées par le pipeline IA — filtre uniquement les codes valides
 * (case-insensible, uppercased). Retourne la liste filtrée non vide, ou
 * `null` si rien ne matche.
 */
export function computeFautesDetectees(input: DivorceFautePrefillInput): FauteCode[] | null {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const raw = (input.aiData as any)?.fautesDetectees;
  if (!Array.isArray(raw) || raw.length === 0) return null;
  const filtered = raw
    .filter((f: unknown): f is string => typeof f === 'string')
    .map((f: string) => f.toUpperCase())
    .filter((f: string): f is FauteCode => VALID_FAUTE_CODES.has(f));
  return filtered.length > 0 ? filtered : null;
}

/**
 * Maître — compte les champs non-null. Strict miroir du runtime
 * `prefillFromAi()` de `DivorceFauteSectionComponent`.
 */
export function computePrefillCount(input: DivorceFautePrefillInput): number {
  let n = 0;
  if (computeDureeMariage(input) !== null) n++;
  if (computeRevenusDemandeur(input) !== null) n++;
  if (computeRevenusDefendeur(input) !== null) n++;
  if (computeDateDepotAssignation(input) !== null) n++;
  if (computeFautesDetectees(input) !== null) n++;
  return n;
}

export const DivorceFautePrefillRules = {
  VALID_FAUTE_CODES,
  computeDureeMariage,
  computeRevenusDemandeur,
  computeRevenusDefendeur,
  computeDateDepotAssignation,
  computeFautesDetectees,
  computePrefillCount,
};
