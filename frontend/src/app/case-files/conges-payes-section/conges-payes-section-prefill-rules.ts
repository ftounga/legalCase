/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Congés payés — indemnité" (FR).
 * SF-246-21 : branchement de 2 champs depuis `paie_detection` (jours acquis, jours pris).
 * Module pur — runtime et static appellent les mêmes fonctions.
 */

export interface CongesPayesPrefillInput {
  aiData?: {
    salaireBrutMensuel?: number | null;
    dateLicenciement?: string | null;
    // SF-246-21 — paie_detection
    congesJoursAcquis?: number | null;
    congesJoursPris?: number | null;
  } | null;
  workspaceCountry?: string;
}

export function computeSalaireMensuelBrutEur(input: CongesPayesPrefillInput): number | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

/**
 * SF-246-22 — Total des rémunérations brutes de la période de référence (base de
 * la méthode du 1/10ᵉ). Aucune source IA directe → **estimation dérivée** du
 * salaire mensuel brut × 12 (période de référence annuelle standard). Marquée
 * « estimation » côté UI + éditable : l'avocat ajuste si la période diffère.
 */
export function computeTotalRemunerationPeriodeEur(input: CongesPayesPrefillInput): number | null {
  const salaire = computeSalaireMensuelBrutEur(input);
  return salaire !== null ? salaire * 12 : null;
}

export function computeDateRupture(input: CongesPayesPrefillInput): string | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.dateLicenciement;
  return typeof v === 'string' && v.length > 0 ? v : null;
}

/** SF-246-21 : jours de congés acquis [0, 50]. Source : bulletins / STC. */
export function computeJoursAcquis(input: CongesPayesPrefillInput): number | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.congesJoursAcquis;
  return typeof v === 'number' && v >= 0 && v <= 50 ? v : null;
}

/** SF-246-21 : jours de congés pris [0, 50]. Distinct des jours acquis. */
export function computeJoursPris(input: CongesPayesPrefillInput): number | null {
  if (input.workspaceCountry !== 'FRANCE') return null;
  const v = input.aiData?.congesJoursPris;
  return typeof v === 'number' && v >= 0 && v <= 50 ? v : null;
}

export function computePrefillCount(input: CongesPayesPrefillInput): number {
  let count = 0;
  if (computeSalaireMensuelBrutEur(input) !== null) count++;
  if (computeTotalRemunerationPeriodeEur(input) !== null) count++;
  if (computeDateRupture(input) !== null) count++;
  if (computeJoursAcquis(input) !== null) count++;
  if (computeJoursPris(input) !== null) count++;
  return count;
}

export const CongesPayesSectionPrefillRules = {
  computeSalaireMensuelBrutEur,
  computeTotalRemunerationPeriodeEur,
  computeDateRupture,
  computeJoursAcquis,
  computeJoursPris,
  computePrefillCount,
};
