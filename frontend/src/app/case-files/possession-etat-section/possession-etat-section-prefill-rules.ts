/**
 * F-236 SF-236-02 — Helper partagé `PossessionEtatPrefillRules`.
 *
 * Le pré-fill est piloté par un **flag agrégé** côté IA
 * (`possessionEtatConforme5AnsDetected === true`) qui pré-coche un faisceau
 * de 5 conditions cardinales (tractatus + fama + continue + paisible +
 * nonEquivoque). `nomen` reste à la saisie manuelle.
 *
 * compteur = 5 quand le flag est true, 0 sinon.
 */
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

export interface PossessionEtatPrefillInput {
  aiData?: Partial<FamilleExtractedData> | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function isFaisceauConforme(input: PossessionEtatPrefillInput): boolean {
  return input.aiData?.possessionEtatConforme5AnsDetected === true;
}

export function computePrefillCount(input: PossessionEtatPrefillInput): number {
  return isFaisceauConforme(input) ? 5 : 0;
}

export const PossessionEtatPrefillRules = {
  isFaisceauConforme,
  computePrefillCount,
};
