/**
 * SF-DT-38-02 + SF-246-29 — Helper partagé pour l'outil "Rupture de période
 * d'essai" (F-DT-38). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 / F-237).
 *
 * SF-DT-38-02 (livraison initiale, 9 champs depuis l'existant) :
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
 * SF-246-29 (pré-fill exhaustif, 14 champs depuis le sous-objet IA
 * `rupture_periode_essai_detail`) :
 *   categorieSocioProfessionnelle           ← aiData.rpeCategorieSocioProfessionnelle
 *   dureeCddMois                            ← aiData.rpeDureeCddMois
 *   dureePeriodeEssaiContractuelleMois      ← aiData.rpeDureePeriodeEssaiMois
 *   renouvellementInvoque                   ← aiData.rpeRenouvellementInvoque
 *   accordBrancheRenouvellement             ← aiData.rpeAccordBrancheRenouvellement
 *   accordEcritSalarieRenouvellement        ← aiData.rpeAccordEcritSalarieRenouvellement
 *   auteurRupture                           ← aiData.rpeAuteurRupture
 *   delaiPrevenanceJoursAppliques           ← aiData.rpeDelaiPrevenanceJours
 *   motifLieAuxCompetencesProfessionnelles  ← aiData.rpeMotifLieCompetences
 *   motifEconomiqueOuOrganisationnel        ← aiData.rpeMotifEconomique
 *   atteinteLiberteFondamentale             ← aiData.rpeAtteinteLiberteFondamentale
 *   lettreRuptureMotivee                    ← aiData.rpeLettreRuptureMotivee
 *   motifsAveresParPieces                   ← aiData.rpeMotifsAveresParPieces
 *   conventionCollectivePlusFavorableRespectee ← aiData.rpeCcnPlusFavorableRespectee
 *
 * FRANCE uniquement (workspaceCountry === 'FRANCE'). Total : 23 champs.
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import {
  AuteurRupture,
  CategorieSocioProfessionnelle,
  DiscriminationMotif,
  TypeContratEssai,
} from '../../core/models/rupture-periode-essai.model';

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

/** Whitelist enums (alignée sur backend `CATEGORIE_SOCIO_PROFESSIONNELLE_CODES`). */
const CATEGORIE_SOCIO_CODES: readonly CategorieSocioProfessionnelle[] = [
  'OUVRIER_EMPLOYE',
  'AGENT_MAITRISE_TECHNICIEN',
  'CADRE',
];

const AUTEUR_RUPTURE_CODES: readonly AuteurRupture[] = ['EMPLOYEUR', 'SALARIE'];

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
    // SF-246-29 — 14 champs IA additionnels (sous-objet rupture_periode_essai_detail)
    | 'rpeCategorieSocioProfessionnelle'
    | 'rpeDureeCddMois'
    | 'rpeDureePeriodeEssaiMois'
    | 'rpeRenouvellementInvoque'
    | 'rpeAccordBrancheRenouvellement'
    | 'rpeAccordEcritSalarieRenouvellement'
    | 'rpeAuteurRupture'
    | 'rpeDelaiPrevenanceJours'
    | 'rpeMotifLieCompetences'
    | 'rpeMotifEconomique'
    | 'rpeAtteinteLiberteFondamentale'
    | 'rpeLettreRuptureMotivee'
    | 'rpeMotifsAveresParPieces'
    | 'rpeCcnPlusFavorableRespectee'
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

// ---------------------------------------------------------------------------
// SF-246-29 — 14 nouvelles fonctions de pré-fill exhaustif
// ---------------------------------------------------------------------------

/** `categorieSocioProfessionnelle` — whitelist 3 codes (L.1221-19 CT). */
export function computeCategorieSocioProfessionnelle(
  input: RupturePeriodeEssaiPrefillInput,
): CategorieSocioProfessionnelle | null {
  if (!isFrance(input)) return null;
  const raw = input.aiData?.rpeCategorieSocioProfessionnelle;
  if (typeof raw !== 'string') return null;
  return (CATEGORIE_SOCIO_CODES as readonly string[]).includes(raw)
    ? (raw as CategorieSocioProfessionnelle)
    : null;
}

/** `dureeCddMois` — entier > 0 (null pour CDI / intérim). */
export function computeDureeCddMois(
  input: RupturePeriodeEssaiPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.rpeDureeCddMois;
  return typeof v === 'number' && Number.isFinite(v) && v > 0 ? v : null;
}

/** `dureePeriodeEssaiContractuelleMois` — entier > 0. */
export function computeDureePeriodeEssaiMois(
  input: RupturePeriodeEssaiPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.rpeDureePeriodeEssaiMois;
  return typeof v === 'number' && Number.isFinite(v) && v > 0 ? v : null;
}

/** `renouvellementInvoque` — booléen tri-état. */
export function computeRenouvellementInvoque(
  input: RupturePeriodeEssaiPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.rpeRenouvellementInvoque;
  return typeof v === 'boolean' ? v : null;
}

/** `accordBrancheRenouvellement` — booléen tri-état. */
export function computeAccordBrancheRenouvellement(
  input: RupturePeriodeEssaiPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.rpeAccordBrancheRenouvellement;
  return typeof v === 'boolean' ? v : null;
}

/** `accordEcritSalarieRenouvellement` — booléen tri-état (L.1221-23 CT). */
export function computeAccordEcritSalarieRenouvellement(
  input: RupturePeriodeEssaiPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.rpeAccordEcritSalarieRenouvellement;
  return typeof v === 'boolean' ? v : null;
}

/** `auteurRupture` — whitelist 2 codes. */
export function computeAuteurRupture(
  input: RupturePeriodeEssaiPrefillInput,
): AuteurRupture | null {
  if (!isFrance(input)) return null;
  const raw = input.aiData?.rpeAuteurRupture;
  if (typeof raw !== 'string') return null;
  return (AUTEUR_RUPTURE_CODES as readonly string[]).includes(raw)
    ? (raw as AuteurRupture)
    : null;
}

/** `delaiPrevenanceJoursAppliques` — entier >= 0. */
export function computeDelaiPrevenanceJours(
  input: RupturePeriodeEssaiPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.rpeDelaiPrevenanceJours;
  return typeof v === 'number' && Number.isFinite(v) && v >= 0 ? v : null;
}

/** `motifLieAuxCompetencesProfessionnelles` — booléen tri-état (Cass. soc. 20/11/2007). */
export function computeMotifLieCompetences(
  input: RupturePeriodeEssaiPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.rpeMotifLieCompetences;
  return typeof v === 'boolean' ? v : null;
}

/** `motifEconomiqueOuOrganisationnel` — booléen tri-état. */
export function computeMotifEconomique(
  input: RupturePeriodeEssaiPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.rpeMotifEconomique;
  return typeof v === 'boolean' ? v : null;
}

/** `atteinteLiberteFondamentale` — texte libre (≤ 500 car.) ; caractérise verdict NULLE. */
export function computeAtteinteLiberteFondamentale(
  input: RupturePeriodeEssaiPrefillInput,
): string | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.rpeAtteinteLiberteFondamentale;
  return typeof v === 'string' && v.trim().length > 0 ? v : null;
}

/** `lettreRuptureMotivee` — booléen tri-état. */
export function computeLettreRuptureMotivee(
  input: RupturePeriodeEssaiPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.rpeLettreRuptureMotivee;
  return typeof v === 'boolean' ? v : null;
}

/** `motifsAveresParPieces` — booléen tri-état (atténuation ILLEGALE → RISQUE_ABUSIVE). */
export function computeMotifsAveresParPieces(
  input: RupturePeriodeEssaiPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.rpeMotifsAveresParPieces;
  return typeof v === 'boolean' ? v : null;
}

/** `conventionCollectivePlusFavorableRespectee` — booléen tri-état. */
export function computeCcnPlusFavorableRespectee(
  input: RupturePeriodeEssaiPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.rpeCcnPlusFavorableRespectee;
  return typeof v === 'boolean' ? v : null;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant.
 *
 * SF-246-29 : 23 champs au total (9 SF-DT-38-02 + 14 SF-246-29).
 */
export function computePrefillCount(input: RupturePeriodeEssaiPrefillInput): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  // SF-DT-38-02 — 9 champs depuis l'existant
  if (computeTypeContrat(input) !== null) count++;
  if (computeDateDebutContrat(input) !== null) count++;
  if (computeDateRupture(input) !== null) count++;
  if (computeMotifInvoque(input) !== null) count++;
  if (computeDiscriminationInvoquee(input) !== null) count++;
  if (computeGrossesseAuMomentRupture(input) !== null) count++;
  if (computeArretAccidentTravail(input) !== null) count++;
  if (computeConventionApplicable(input) !== null) count++;
  if (computeSalaireMensuelBrut(input) !== null) count++;
  // SF-246-29 — 14 champs IA additionnels
  if (computeCategorieSocioProfessionnelle(input) !== null) count++;
  if (computeDureeCddMois(input) !== null) count++;
  if (computeDureePeriodeEssaiMois(input) !== null) count++;
  if (computeRenouvellementInvoque(input) !== null) count++;
  if (computeAccordBrancheRenouvellement(input) !== null) count++;
  if (computeAccordEcritSalarieRenouvellement(input) !== null) count++;
  if (computeAuteurRupture(input) !== null) count++;
  if (computeDelaiPrevenanceJours(input) !== null) count++;
  if (computeMotifLieCompetences(input) !== null) count++;
  if (computeMotifEconomique(input) !== null) count++;
  if (computeAtteinteLiberteFondamentale(input) !== null) count++;
  if (computeLettreRuptureMotivee(input) !== null) count++;
  if (computeMotifsAveresParPieces(input) !== null) count++;
  if (computeCcnPlusFavorableRespectee(input) !== null) count++;
  return count;
}

export const RupturePeriodeEssaiSectionPrefillRules = {
  // SF-DT-38-02
  computeTypeContrat,
  computeDateDebutContrat,
  computeDateRupture,
  computeMotifInvoque,
  computeDiscriminationInvoquee,
  computeGrossesseAuMomentRupture,
  computeArretAccidentTravail,
  computeConventionApplicable,
  computeSalaireMensuelBrut,
  // SF-246-29
  computeCategorieSocioProfessionnelle,
  computeDureeCddMois,
  computeDureePeriodeEssaiMois,
  computeRenouvellementInvoque,
  computeAccordBrancheRenouvellement,
  computeAccordEcritSalarieRenouvellement,
  computeAuteurRupture,
  computeDelaiPrevenanceJours,
  computeMotifLieCompetences,
  computeMotifEconomique,
  computeAtteinteLiberteFondamentale,
  computeLettreRuptureMotivee,
  computeMotifsAveresParPieces,
  computeCcnPlusFavorableRespectee,
  computePrefillCount,
};
