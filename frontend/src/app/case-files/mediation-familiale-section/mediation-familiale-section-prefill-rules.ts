/**
 * F-236 SF-236-02 — Helper partagé `MediationFamilialePrefillRules`.
 *
 * SF-246-27 : 1 champ — motifSaisine.
 * Source réelle : `ai.motifSaisineMediationDetected` (whitelist fermée 5 codes).
 * Fallback : `ai.motifSaisineMediationDetecte` (aspirationnel, rétro-compat).
 * Whitelist stricte : AUTORITE_PARENTALE | CONTRIBUTION_ENTRETIEN |
 *   DROIT_VISITE | RESIDENCE | AUTRE. Codes hors whitelist → null (fail-open).
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { MediationMotifSaisine } from '../../core/models/mediation-familiale.model';

const VALID_MOTIFS: ReadonlySet<string> = new Set<string>([
  'AUTORITE_PARENTALE',
  'CONTRIBUTION_ENTRETIEN',
  'DROIT_VISITE',
  'RESIDENCE',
  'AUTRE',
]);

type Ai = Partial<FamilleExtractedData> & {
  motifSaisineMediationDetecte?: string | null;
};

export interface MediationFamilialePrefillInput {
  aiData?: Ai | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computeMotifSaisine(input: MediationFamilialePrefillInput): MediationMotifSaisine | null {
  const ai = input.aiData;
  if (!ai) return null;
  // SF-246-27 : source réelle `motifSaisineMediationDetected` (prioritaire).
  // Fallback sur `motifSaisineMediationDetecte` (aspirationnel, rétro-compat).
  const raw = (ai as any).motifSaisineMediationDetected ?? ai.motifSaisineMediationDetecte;
  if (typeof raw !== 'string' || raw.length === 0) return null;
  const upper = raw.trim().toUpperCase();
  return VALID_MOTIFS.has(upper) ? (upper as MediationMotifSaisine) : null;
}

export function computePrefillCount(input: MediationFamilialePrefillInput): number {
  return computeMotifSaisine(input) !== null ? 1 : 0;
}

export const MediationFamilialePrefillRules = {
  VALID_MOTIFS,
  computeMotifSaisine,
  computePrefillCount,
};
