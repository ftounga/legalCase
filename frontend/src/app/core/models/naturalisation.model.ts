/**
 * SF-IM-13-02 : types frontend pour l'outil décisionnel "Naturalisation française"
 * (F-IM-13). FR uniquement (Code civil art. 21+).
 *
 * Contrat figé dans SF-IM-13-01 (backend, mergé PR #639).
 *
 * 6 voies du Code civil :
 * - DECRET         : art. 21-15+ (voie classique, 5 ans résidence ou 2 ans si études FR)
 * - MARIAGE        : art. 21-2 (4 ans si cohabitation continue, 5 ans sinon)
 * - ASCENDANT      : art. 21-13-1 (≥ 65 ans + 25 ans résidence + lien français)
 * - MINEUR         : art. 22-1 (parent acquiert + cohabitation habituelle)
 * - REINTEGRATION  : art. 24-1+ (ancien français)
 * - OPPOSITION     : art. 21-4 / 27-2 (info-only — voie de rejet gouvernemental)
 */

/** Code des voies de naturalisation supportées par le calculateur backend. */
export type VoieNaturalisationCode =
  | 'DECRET'
  | 'MARIAGE'
  | 'ASCENDANT'
  | 'MINEUR'
  | 'REINTEGRATION'
  | 'OPPOSITION';

/** Verdict de recevabilité (scoring backend). */
export type VerdictRecevabilite = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

/** Libellés humains pour mat-radio voie. */
export const VOIE_NATURALISATION_LABELS:
  ReadonlyArray<{ code: VoieNaturalisationCode; label: string; sub: string }> = [
    {
      code: 'DECRET',
      label: 'Naturalisation par décret',
      sub: 'Cciv art. 21-15+ — voie classique (5 ans résidence, ou 2 ans études FR)',
    },
    {
      code: 'MARIAGE',
      label: 'Déclaration par mariage',
      sub: 'Cciv art. 21-2 — 4 ans si cohabitation continue (sinon 5 ans)',
    },
    {
      code: 'ASCENDANT',
      label: 'Déclaration ascendant français',
      sub: 'Cciv art. 21-13-1 — ≥ 65 ans + 25 ans résidence + ascendant direct',
    },
    {
      code: 'MINEUR',
      label: 'Effet collectif sur mineur',
      sub: 'Cciv art. 22-1 — parent acquiert + résidence habituelle commune',
    },
    {
      code: 'REINTEGRATION',
      label: 'Réintégration',
      sub: 'Cciv art. 24-1+ — anciens français ayant perdu la nationalité',
    },
    {
      code: 'OPPOSITION',
      label: 'Opposition gouvernementale',
      sub: 'Cciv art. 21-4 / 27-2 — voie de rejet, recours en Conseil d\'État',
    },
  ];

export interface NaturalisationRequest {
  voieNaturalisation: VoieNaturalisationCode;
  /** ≥ 0, requis DECRET / ASCENDANT. */
  dureeResidenceReguliereAnnees?: number | null;
  /** ≥ 0, requis MARIAGE. */
  dureeMariageAnnees?: number | null;
  /** Boolean MARIAGE — impacte délai 4 / 5 ans. */
  cohabitationContinue?: boolean | null;
  /** ≥ 0, requis ASCENDANT (≥ 65). */
  ageDemandeur?: number | null;
  /** Boolean ASCENDANT. */
  ascendantDirectFrancais?: boolean | null;
  /** Boolean MINEUR. */
  parentAcquiertNationalite?: boolean | null;
  /** Boolean MINEUR. */
  vitAvecParentAcquereur?: boolean | null;
  /** Boolean REINTEGRATION. */
  ancienFrancais?: boolean | null;
  /** Boolean default true — critère commun moralité. */
  casierJudiciaireVierge?: boolean | null;
  /** Boolean DECRET / MARIAGE — assimilation linguistique B1. */
  assimilationLangueB1?: boolean | null;
  /** Boolean DECRET — ressources stables et suffisantes. */
  ressourcesStables?: boolean | null;
  /** Boolean default false — bloque toute voie. */
  oppositionGouvernementaleActive?: boolean | null;
  /** Boolean DECRET — réduit durée à 2 ans (Cciv art. 21-18). */
  etudesSuperieuresFrance?: boolean | null;
}

/**
 * SF-206-02 hotfix master-red : la Response ré-expose le snapshot d'inputs
 * Request (ré-édition du formulaire après rechargement). Le composant
 * `naturalisation-section` hydrate déjà ces champs depuis `r.*`. Aligne le
 * contrat sur le pattern F-DT-36 / F-DT-42.
 */
export interface NaturalisationResponse extends NaturalisationRequest {
  caseFileId: string;
  country: 'FRANCE';
  voieRecommandee: string;
  verdictRecevabilite: VerdictRecevabilite;
  criteresNonRemplis: string[];
  documentsAFournir: string[];
  /** 6-18 mois selon la voie. */
  delaiInstructionMois: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
}

/** Libellé humain depuis un code voie. */
export function voieLabel(code: VoieNaturalisationCode | string | null | undefined): string {
  if (!code) return '';
  const opt = VOIE_NATURALISATION_LABELS.find((o) => o.code === code);
  return opt?.label ?? String(code);
}

/**
 * Mappe une chaîne IA libre (procedureChecks / questions) vers l'enum frontend.
 * Normalisation insensible à la casse, accents et tirets/underscores.
 * Retourne `null` si non reconnu (no-op gracieux pour la validation F-IA-03).
 *
 * Mapping pragmatique :
 * - DECRET, NATURALISATION_DECRET → DECRET
 * - MARIAGE, DECLARATION_MARIAGE → MARIAGE
 * - ASCENDANT, DECLARATION_ASCENDANT_FRANCAIS → ASCENDANT
 * - MINEUR, NATURALISATION_MINEUR, EFFET_COLLECTIF → MINEUR
 * - REINTEGRATION → REINTEGRATION
 * - OPPOSITION, OPPOSITION_GOUVERNEMENTALE → OPPOSITION
 */
export function mapVoieFromIa(value: string | null | undefined): VoieNaturalisationCode | null {
  if (!value) return null;
  const normalized = value.toString().trim().toUpperCase()
    .replace(/[\s\-]+/g, '_')
    .replace(/[ÉÈÊË]/g, 'E')
    .replace(/[ÀÂÄ]/g, 'A')
    .replace(/[ÎÏ]/g, 'I')
    .replace(/[ÔÖ]/g, 'O')
    .replace(/[ÙÛÜ]/g, 'U');

  if (normalized === 'DECRET' || normalized === 'NATURALISATION_DECRET'
      || normalized === 'NATURALISATION_PAR_DECRET') {
    return 'DECRET';
  }
  if (normalized === 'MARIAGE' || normalized === 'DECLARATION_MARIAGE'
      || normalized === 'NATURALISATION_MARIAGE'
      || normalized === 'DECLARATION_PAR_MARIAGE') {
    return 'MARIAGE';
  }
  if (normalized === 'ASCENDANT' || normalized === 'DECLARATION_ASCENDANT_FRANCAIS'
      || normalized === 'ASCENDANT_FRANCAIS') {
    return 'ASCENDANT';
  }
  if (normalized === 'MINEUR' || normalized === 'NATURALISATION_MINEUR'
      || normalized === 'EFFET_COLLECTIF' || normalized === 'EFFET_COLLECTIF_MINEUR') {
    return 'MINEUR';
  }
  if (normalized === 'REINTEGRATION' || normalized === 'REINTEGRATION_NATIONALITE') {
    return 'REINTEGRATION';
  }
  if (normalized === 'OPPOSITION' || normalized === 'OPPOSITION_GOUVERNEMENTALE') {
    return 'OPPOSITION';
  }
  return null;
}
