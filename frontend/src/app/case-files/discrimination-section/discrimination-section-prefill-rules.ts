/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Discrimination — dommages-intérêts".
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Logique miroir : `salaireMensuelReference ← aiData.salaireBrutMensuel > 0`.
 */

export interface DiscriminationPrefillInput {
  aiData?: { salaireBrutMensuel?: number | null } | null;
}

export function computeSalaireMensuelReference(input: DiscriminationPrefillInput): number | null {
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computePrefillCount(input: DiscriminationPrefillInput): number {
  let count = 0;
  if (computeSalaireMensuelReference(input) !== null) count++;
  return count;
}

export const DiscriminationSectionPrefillRules = {
  computeSalaireMensuelReference,
  computePrefillCount,
};
