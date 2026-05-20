/**
 * SF-DT-38-02 — Helper partagé pour l'outil "Rupture de période d'essai"
 * (F-DT-38). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 / F-237).
 *
 * Cette première livraison utilise UNIQUEMENT les champs déjà extraits par
 * le pipeline (mapping sans extension du prompt ni du record
 * `TravailExtractedData`) — pré-fill exhaustif F-246 différé en SF dédiée
 * (cf. mini-spec SF-DT-38-02 §"Pré-fill IA").
 *
 *   typeContrat                ← aiData.typeContrat (CDI/CDD/INTERIM, fallback CDI)
 *   dateDebutContrat           ← aiData.dateEntree (ISO)
 *   dateRupture                ← aiData.dateLicenciement (ISO)
 *   motifInvoque               ← aiData.motifLicenciement
 *   discriminationInvoquee     ← aiData.motifNullitePressenti (DISCRIMINATION/SYNDICAL/SANTE…)
 *   grossesseAuMomentRupture   ← aiData.motifNullitePressenti = MATERNITE_PATERNITE
 *   arretAccidentTravailEnCours ← aiData.atMpDetecte
 *   conventionCollectiveApplicable ← (aiData.conventionCollective != null)
 *   salaireMensuelBrut         ← aiData.salaireBrutMensuel
 *
 * FRANCE uniquement (workspaceCountry === 'FRANCE').
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import {
  DiscriminationMotif,
  TypeContratEssai,
} from '../../core/models/rupture-periode-essai.model';

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

/** Sous-ensemble consommé par le pré-fill. */
export interface RupturePeriodeEssaiPrefillInput {
  aiData?: Pick<
    TravailExtractedData,
    | 'typeContrat'
    | 'dateEntree'
    | 'dateLicenciement'
    | 'motifLicenciement'
    | 'motifNullitePressenti'
    | 'atMpDetecte'
    | 'conventionCollective'
    | 'salaireBrutMensuel'
  > | null;
  workspaceCountry?: string;
}

function isFrance(input: RupturePeriodeEssaiPrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

/** `typeContrat` — CDI / CDD / INTERIM (whitelist), fallback CDI si inconnu. */
export function computeTypeContrat(input: RupturePeriodeEssaiPrefillInput): TypeContratEssai | null {
  if (!isFrance(input)) return null;
  const raw = input.aiData?.typeContrat;
  if (typeof raw !== 'string') return null;
  const up = raw.trim().toUpperCase();
  if (up === 'CDI' || up === 'CDD' || up === 'INTERIM') return up as TypeContratEssai;
  return null;
}

/** `dateDebutContrat` — date d'entrée du salarié. */
export function computeDateDebutContrat(input: RupturePeriodeEssaiPrefillInput): string | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.dateEntree;
  return typeof v === 'string' && ISO_DATE_REGEX.test(v) ? v : null;
}

/** `dateRupture` — date de licenciement / rupture. */
export function computeDateRupture(input: RupturePeriodeEssaiPrefillInput): string | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.dateLicenciement;
  return typeof v === 'string' && ISO_DATE_REGEX.test(v) ? v : null;
}

/** `motifInvoque` — texte libre du motif. */
export function computeMotifInvoque(input: RupturePeriodeEssaiPrefillInput): string | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.motifLicenciement;
  return typeof v === 'string' && v.trim().length > 0 ? v : null;
}

/**
 * `discriminationInvoquee` — mapping du `motifNullitePressenti` IA vers
 * l'enum F-DT-38 (subset de L.1132-1). MATERNITE_PATERNITE est traité
 * séparément par `computeGrossesse`.
 */
export function computeDiscriminationInvoquee(
  input: RupturePeriodeEssaiPrefillInput,
): DiscriminationMotif | null {
  if (!isFrance(input)) return null;
  const motif = input.aiData?.motifNullitePressenti;
  if (!motif) return null;
  switch (motif) {
    case 'DISCRIMINATION':
      return 'AUTRE';
    case 'HARCELEMENT_MORAL':
    case 'HARCELEMENT_SEXUEL':
      return 'SEXE';
    case 'SYNDICAL':
      return 'SYNDICAL';
    case 'ACCIDENT_MP':
      return 'SANTE';
    case 'MATERNITE_PATERNITE':
      // Mappé via computeGrossesse (champ dédié grossesseAuMomentRupture)
      return null;
    case 'RETORSION':
    default:
      return null;
  }
}

/** `grossesseAuMomentRupture` — true si `motifNullitePressenti = MATERNITE_PATERNITE`. */
export function computeGrossesseAuMomentRupture(
  input: RupturePeriodeEssaiPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const motif = input.aiData?.motifNullitePressenti;
  if (motif === 'MATERNITE_PATERNITE') return true;
  return null;
}

/** `arretAccidentTravailEnCours` — flag F-DT-33 already extracted. */
export function computeArretAccidentTravail(
  input: RupturePeriodeEssaiPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.atMpDetecte;
  return typeof v === 'boolean' ? v : null;
}

/** `conventionCollectiveApplicable` — true si CCN identifiée. */
export function computeConventionApplicable(
  input: RupturePeriodeEssaiPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.conventionCollective;
  if (v === undefined) return null;
  return v != null && v.trim().length > 0;
}

/** `salaireMensuelBrut` — déjà extrait (avec déduction × 1,30 si net). */
export function computeSalaireMensuelBrut(
  input: RupturePeriodeEssaiPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant.
 */
export function computePrefillCount(input: RupturePeriodeEssaiPrefillInput): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeTypeContrat(input) !== null) count++;
  if (computeDateDebutContrat(input) !== null) count++;
  if (computeDateRupture(input) !== null) count++;
  if (computeMotifInvoque(input) !== null) count++;
  if (computeDiscriminationInvoquee(input) !== null) count++;
  if (computeGrossesseAuMomentRupture(input) !== null) count++;
  if (computeArretAccidentTravail(input) !== null) count++;
  if (computeConventionApplicable(input) !== null) count++;
  if (computeSalaireMensuelBrut(input) !== null) count++;
  return count;
}

export const RupturePeriodeEssaiSectionPrefillRules = {
  computeTypeContrat,
  computeDateDebutContrat,
  computeDateRupture,
  computeMotifInvoque,
  computeDiscriminationInvoquee,
  computeGrossesseAuMomentRupture,
  computeArretAccidentTravail,
  computeConventionApplicable,
  computeSalaireMensuelBrut,
  computePrefillCount,
};
