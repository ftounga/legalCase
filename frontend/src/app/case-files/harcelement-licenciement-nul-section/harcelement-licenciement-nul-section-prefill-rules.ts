/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Harcèlement — licenciement nul".
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Logique miroir :
 *   `salaireMensuelReference ← aiData.salaireBrutMensuel > 0`
 *   `motifNullite            ← AI_MOTIF_TO_MOTIF_NULLITE_FR[motifNullitePressenti]`
 *                              (FRANCE uniquement)
 */

import { MotifNulliteFr } from '../../core/models/harcelement-nullite.model';

const AI_MOTIF_TO_MOTIF_NULLITE_FR: Readonly<Record<string, MotifNulliteFr>> = {
  DISCRIMINATION: 'DISCRIMINATION',
  HARCELEMENT_MORAL: 'HARCELEMENT_MORAL',
  HARCELEMENT_SEXUEL: 'HARCELEMENT_SEXUEL',
  MATERNITE_PATERNITE: 'GROSSESSE',
};

export interface HarcLicNulPrefillInput {
  aiData?: {
    salaireBrutMensuel?: number | null;
    motifNullitePressenti?: string | null;
  } | null;
  workspaceCountry?: string;
}

export function computeSalaireMensuelReference(input: HarcLicNulPrefillInput): number | null {
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computeMotifNullite(input: HarcLicNulPrefillInput): MotifNulliteFr | null {
  // Gate pays : motifNullitePressenti n'est rempli qu'en France (cf. backend).
  if (input.workspaceCountry !== 'FRANCE') return null;
  const raw = input.aiData?.motifNullitePressenti;
  if (typeof raw !== 'string' || raw.length === 0) return null;
  return AI_MOTIF_TO_MOTIF_NULLITE_FR[raw.toUpperCase()] ?? null;
}

export function computePrefillCount(input: HarcLicNulPrefillInput): number {
  let count = 0;
  if (computeSalaireMensuelReference(input) !== null) count++;
  if (computeMotifNullite(input) !== null) count++;
  return count;
}

export const HarcelementLicenciementNulSectionPrefillRules = {
  computeSalaireMensuelReference,
  computeMotifNullite,
  computePrefillCount,
  AI_MOTIF_TO_MOTIF_NULLITE_FR,
};
