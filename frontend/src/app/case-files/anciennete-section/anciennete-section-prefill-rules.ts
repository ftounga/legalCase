/**
 * F-236 SF-236-02 — Helper partagé pour le pré-fill IA de l'outil Ancienneté.
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Logique miroir de `AncienneteSectionComponent.prefillFromAi()` (champs IA
 * uniquement — le bareme et le service Convention sont consultés runtime-only).
 *   `conventionCode    ← normalize(aiData.conventionCollective)`
 *   `dateEntree        ← aiData.dateEntree`
 *   `salaireBase       ← aiData.salaireBrutMensuel`
 *   `congesContrat     ← aiData.congesContractuels`
 *   `primeContrat      ← aiData.primeAncienneteContractuelle`
 */

import { ConventionReferentialService } from '../../core/services/convention-referential.service';

export interface AnciennetePrefillInput {
  aiData?: {
    conventionCollective?: string | null;
    dateEntree?: string | null;
    salaireBrutMensuel?: number | null;
    congesContractuels?: number | null;
    primeAncienneteContractuelle?: number | null;
  } | null;
}

export function computeConventionCode(input: AnciennetePrefillInput): string | null {
  const ai = input.aiData;
  if (!ai?.conventionCollective) return null;
  return ConventionReferentialService.normalizeCode(ai.conventionCollective);
}

export function computeDateEntree(input: AnciennetePrefillInput): string | null {
  const v = input.aiData?.dateEntree;
  return typeof v === 'string' && v.length > 0 ? v : null;
}

export function computeSalaireBase(input: AnciennetePrefillInput): number | null {
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computeCongesContrat(input: AnciennetePrefillInput): number | null {
  const v = input.aiData?.congesContractuels;
  return typeof v === 'number' ? v : null;
}

export function computePrimeContrat(input: AnciennetePrefillInput): number | null {
  const v = input.aiData?.primeAncienneteContractuelle;
  return typeof v === 'number' ? v : null;
}

export function computePrefillCount(input: AnciennetePrefillInput): number {
  let count = 0;
  if (computeConventionCode(input) !== null) count++;
  if (computeDateEntree(input) !== null) count++;
  if (computeSalaireBase(input) !== null) count++;
  if (computeCongesContrat(input) !== null) count++;
  if (computePrimeContrat(input) !== null) count++;
  return count;
}

export const AncienneteSectionPrefillRules = {
  computeConventionCode,
  computeDateEntree,
  computeSalaireBase,
  computeCongesContrat,
  computePrimeContrat,
  computePrefillCount,
};
