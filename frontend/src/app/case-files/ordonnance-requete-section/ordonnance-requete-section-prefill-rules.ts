/**
 * F-236 SF-236-02 — Helper partagé `OrdonnanceRequetePrefillRules`.
 *
 * 1 champ : presenceEnfantsDetected. Compté UNIQUEMENT si IA détecte `true`
 * (parité avec la pose de provenance IA côté runtime — `false` ne pose pas
 * le badge "Pré-rempli").
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

type Ai = Partial<FamilleExtractedData> & {
  presenceEnfantsDetected?: boolean | null;
};

export interface OrdonnanceRequetePrefillInput {
  aiData?: Ai | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function isPresenceEnfantsTrue(input: OrdonnanceRequetePrefillInput): boolean {
  return input.aiData?.presenceEnfantsDetected === true;
}

export function computePrefillCount(input: OrdonnanceRequetePrefillInput): number {
  return isPresenceEnfantsTrue(input) ? 1 : 0;
}

export const OrdonnanceRequetePrefillRules = {
  isPresenceEnfantsTrue,
  computePrefillCount,
};
