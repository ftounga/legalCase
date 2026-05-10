/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Licenciement nul détection".
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Logique miroir :
 *   `salaireMensuelBrutEur          ← aiData.salaireBrutMensuel > 0`
 *   `dateNotificationLicenciement   ← aiData.dateLicenciement (ISO YYYY-MM-DD)`
 *   `protectionFlag (1 booléen)     ← AI_MOTIF_TO_PROTECTION_FLAG[motifNullitePressenti]`
 */

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

export const AI_MOTIF_TO_PROTECTION_FLAG: Readonly<Record<string, string>> = {
  HARCELEMENT_MORAL: 'salarieHarceleAvere',
  HARCELEMENT_SEXUEL: 'salarieHarceleAvere',
  DISCRIMINATION: 'salarieDiscriminationAlleguee',
  MATERNITE_PATERNITE: 'salarieEnceinte',
  ACCIDENT_MP: 'salarieAccidentTravail',
  RETORSION: 'salarieActionJustice',
  SYNDICAL: 'salarieMandatRepresentant',
};

export interface LicenciementNulDetectionPrefillInput {
  aiData?: {
    salaireBrutMensuel?: number | null;
    dateLicenciement?: string | null;
    motifNullitePressenti?: string | null;
  } | null;
}

export function computeSalaire(input: LicenciementNulDetectionPrefillInput): number | null {
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

export function computeDateNotification(input: LicenciementNulDetectionPrefillInput): string | null {
  const v = input.aiData?.dateLicenciement;
  return typeof v === 'string' && ISO_DATE_REGEX.test(v) ? v : null;
}

export function computeProtectionFlag(input: LicenciementNulDetectionPrefillInput): string | null {
  const v = input.aiData?.motifNullitePressenti;
  if (typeof v !== 'string' || v.length === 0) return null;
  return AI_MOTIF_TO_PROTECTION_FLAG[v.toUpperCase()] ?? null;
}

export function computePrefillCount(input: LicenciementNulDetectionPrefillInput): number {
  let count = 0;
  if (computeSalaire(input) !== null) count++;
  if (computeDateNotification(input) !== null) count++;
  if (computeProtectionFlag(input) !== null) count++;
  return count;
}

export const LicenciementNulDetectionSectionPrefillRules = {
  computeSalaire,
  computeDateNotification,
  computeProtectionFlag,
  computePrefillCount,
  AI_MOTIF_TO_PROTECTION_FLAG,
};
