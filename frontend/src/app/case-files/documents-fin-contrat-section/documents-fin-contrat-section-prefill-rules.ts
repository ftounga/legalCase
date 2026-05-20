/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Documents fin de contrat" (FR).
 * SF-246-21 : branchement de 3 champs depuis `procedure_details_detection`
 * (dateCertificatTravail, dateAttestationFranceTravail, dateSoldeToutCompte).
 * Module pur — runtime et static appellent les mêmes fonctions.
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export interface DocumentsFinContratPrefillInput {
  aiData?: {
    salaireBrutMensuel?: number | null;
    dateLicenciement?: string | null;
    // SF-246-21 — procedure_details_detection
    documentsDateCertificatTravail?: string | null;
    documentsDateAttestationFranceTravail?: string | null;
    documentsDateSoldeToutCompte?: string | null;
  } | null;
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

/** SF-246-21 : date du certificat de travail (ISO). */
export function computeDateCertificatTravail(input: DocumentsFinContratPrefillInput): string | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.documentsDateCertificatTravail;
  return typeof v === 'string' && ISO_DATE_RE.test(v) ? v : null;
}

/** SF-246-21 : date de l'attestation France Travail (ISO). */
export function computeDateAttestationFranceTravail(input: DocumentsFinContratPrefillInput): string | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.documentsDateAttestationFranceTravail;
  return typeof v === 'string' && ISO_DATE_RE.test(v) ? v : null;
}

/** SF-246-21 : date du solde de tout compte (ISO). */
export function computeDateSoldeToutCompte(input: DocumentsFinContratPrefillInput): string | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.documentsDateSoldeToutCompte;
  return typeof v === 'string' && ISO_DATE_RE.test(v) ? v : null;
}

export function computePrefillCount(input: DocumentsFinContratPrefillInput): number {
  let count = 0;
  if (computeSalaireMensuelBrutEur(input) !== null) count++;
  if (computeDateFinContrat(input) !== null) count++;
  if (computeDateCertificatTravail(input) !== null) count++;
  if (computeDateAttestationFranceTravail(input) !== null) count++;
  if (computeDateSoldeToutCompte(input) !== null) count++;
  return count;
}

export const DocumentsFinContratSectionPrefillRules = {
  computeSalaireMensuelBrutEur,
  computeDateFinContrat,
  computeDateCertificatTravail,
  computeDateAttestationFranceTravail,
  computeDateSoldeToutCompte,
  computePrefillCount,
};
