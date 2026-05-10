/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Documents fin de contrat" (FR).
 * Module pur — runtime et static appellent les mêmes fonctions.
 */

export interface DocumentsFinContratPrefillInput {
  aiData?: { salaireBrutMensuel?: number | null; dateLicenciement?: string | null } | null;
  workspaceCountry?: string;
}

export function computeSalaireMensuelBrutEur(input: DocumentsFinContratPrefillInput): number | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computeDateFinContrat(input: DocumentsFinContratPrefillInput): string | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.dateLicenciement;
  return typeof v === 'string' && v.length > 0 ? v : null;
}

export function computePrefillCount(input: DocumentsFinContratPrefillInput): number {
  let count = 0;
  if (computeSalaireMensuelBrutEur(input) !== null) count++;
  if (computeDateFinContrat(input) !== null) count++;
  return count;
}

export const DocumentsFinContratSectionPrefillRules = {
  computeSalaireMensuelBrutEur,
  computeDateFinContrat,
  computePrefillCount,
};
