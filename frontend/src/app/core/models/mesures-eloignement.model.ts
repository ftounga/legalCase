/**
 * SF-IM-20-02 : types frontend pour l'outil décisionnel "Mesures d'éloignement
 * avancées" (F-IM-20). FR uniquement (CESEDA art. L.631+, L.612+, L.222+).
 *
 * Contrat figé dans SF-IM-20-01 (backend, mergé PR #645).
 *
 * 5 dispositifs :
 * - EXPULSION_PREFECTORALE  : CESEDA L.631-1 — menace grave à l'ordre public, recours TA 30j.
 * - EXPULSION_MINISTERIELLE : CESEDA L.631-2 — urgence absolue, recours CE 60j.
 * - EXPULSION_SECURITE_ETAT : CESEDA L.631-3 — sûreté de l'État, recours CE 60j.
 * - IRTF                    : CESEDA L.612-6+ — interdiction de retour, recours TA 15j (suspensif via OQTF).
 * - IAT                     : CESEDA L.222-1+ — interdiction administrative du territoire, recours CE 60j.
 */

/** Code dispositif d'éloignement (enum fermé backend). */
export type DispositifEloignementCode =
  | 'EXPULSION_PREFECTORALE'
  | 'EXPULSION_MINISTERIELLE'
  | 'EXPULSION_SECURITE_ETAT'
  | 'IRTF'
  | 'IAT';

/** Code motif menace (enum fermé backend). */
export type MotifMenaceCode =
  | 'ORDRE_PUBLIC'
  | 'SECURITE_ETAT'
  | 'TERRORISME'
  | 'RECIDIVE_GRAVE'
  | 'AUTRE';

/** Verdict de légalité (scoring backend). */
export type VerdictLegaliteEloignement = 'VALIDE' | 'CONTESTABLE' | 'NUL';

/** Juridiction de recours. */
export type JuridictionRecours = 'TA' | 'CE';

/** Libellés humains pour mat-radio dispositif. */
export const DISPOSITIF_ELOIGNEMENT_LABELS:
  ReadonlyArray<{ code: DispositifEloignementCode; label: string; sub: string }> = [
    {
      code: 'EXPULSION_PREFECTORALE',
      label: 'Expulsion préfectorale',
      sub: 'CESEDA art. L.631-1 — menace grave à l\'ordre public, recours TA 30j',
    },
    {
      code: 'EXPULSION_MINISTERIELLE',
      label: 'Expulsion ministérielle',
      sub: 'CESEDA art. L.631-2 — urgence absolue, recours CE 60j',
    },
    {
      code: 'EXPULSION_SECURITE_ETAT',
      label: 'Expulsion sûreté de l\'État',
      sub: 'CESEDA art. L.631-3 — nécessité impérieuse pour la sûreté de l\'État, recours CE 60j',
    },
    {
      code: 'IRTF',
      label: 'IRTF — Interdiction de retour',
      sub: 'CESEDA art. L.612-6+ — sanction associée à OQTF, recours TA 15j (suspensif)',
    },
    {
      code: 'IAT',
      label: 'IAT — Interdiction administrative du territoire',
      sub: 'CESEDA art. L.222-1+ — interdiction préventive d\'entrée, recours CE 60j',
    },
  ];

/** Libellés humains pour mat-radio motif menace. */
export const MOTIF_MENACE_LABELS:
  ReadonlyArray<{ code: MotifMenaceCode; label: string }> = [
    { code: 'ORDRE_PUBLIC',   label: 'Trouble grave à l\'ordre public' },
    { code: 'SECURITE_ETAT',  label: 'Sécurité de l\'État' },
    { code: 'TERRORISME',     label: 'Terrorisme' },
    { code: 'RECIDIVE_GRAVE', label: 'Récidive grave' },
    { code: 'AUTRE',          label: 'Autre motif' },
  ];

export interface MesuresEloignementRequest {
  dispositif: DispositifEloignementCode;
  motifMenace: MotifMenaceCode;
  /** EXPULSION_* — default true. */
  procedureCommissionRespectee?: boolean | null;
  /** EXPULSION_* — default false. */
  urgenceAbsolueJustifiee?: boolean | null;
  /** IRTF — entier ≥ 0 (mois). */
  dureeCircularitePrecaire?: number | null;
  /** IRTF — entier ≥ 0 (mois). */
  dureePresenceIrreguliereMois?: number | null;
  /** IRTF — comportement justifiant. */
  comportementAggravant?: boolean | null;
  /** Date de dépôt prévue / effective du recours, ≤ +1 an. */
  recoursDelai?: string | null;
}

export interface MesuresEloignementResponse {
  caseFileId: string;
  country: 'FRANCE';
  dispositif: string;
  dispositifRecommande: string;
  motifMenace: string;
  procedureCommissionRespectee?: boolean | null;
  urgenceAbsolueJustifiee?: boolean | null;
  comportementAggravant?: boolean | null;
  verdictLegalite: VerdictLegaliteEloignement;
  risqueAnnulation: string[];
  /** 15 / 30 / 60 selon dispositif. */
  delaiRecoursJours: number;
  juridictionRecours: JuridictionRecours;
  documentsRequis: string[];
  baseJuridique: string;
  formule: string;
  messages: string[];
}

/** Libellé humain depuis un code dispositif (fallback = code brut). */
export function dispositifLabel(code: DispositifEloignementCode | string | null | undefined): string {
  if (!code) return '';
  const opt = DISPOSITIF_ELOIGNEMENT_LABELS.find((o) => o.code === code);
  return opt?.label ?? String(code);
}

/** Libellé humain depuis un code motif menace (fallback = code brut). */
export function motifMenaceLabel(code: MotifMenaceCode | string | null | undefined): string {
  if (!code) return '';
  const opt = MOTIF_MENACE_LABELS.find((o) => o.code === code);
  return opt?.label ?? String(code);
}

/**
 * Mappe une chaîne IA libre (typeProcedureDetectee / procedureChecks /
 * questions) vers l'enum frontend `DispositifEloignementCode`. Normalisation
 * insensible à la casse, accents, tirets/underscores. Retourne `null` si
 * non reconnu (no-op gracieux pour la validation F-IA-03).
 *
 * Mapping pragmatique :
 * - EXPULSION, EXPULSION_PREFECTORALE → EXPULSION_PREFECTORALE
 * - EXPULSION_MINISTERIELLE, EXPULSION_MINISTRE → EXPULSION_MINISTERIELLE
 * - EXPULSION_SECURITE_ETAT, EXPULSION_SURETE_ETAT → EXPULSION_SECURITE_ETAT
 * - IRTF, INTERDICTION_RETOUR, INTERDICTION_DE_RETOUR → IRTF
 * - IAT, INTERDICTION_ADMINISTRATIVE_TERRITOIRE → IAT
 */
export function mapDispositifFromIa(
  value: string | null | undefined,
): DispositifEloignementCode | null {
  if (!value) return null;
  const n = value.toString().trim().toUpperCase()
    .replace(/[\s\-]+/g, '_')
    .replace(/[ÉÈÊË]/g, 'E')
    .replace(/[ÀÂÄ]/g, 'A')
    .replace(/[ÎÏ]/g, 'I')
    .replace(/[ÔÖ]/g, 'O')
    .replace(/[ÙÛÜ]/g, 'U');

  if (n === 'IRTF' || n === 'INTERDICTION_RETOUR'
      || n === 'INTERDICTION_DE_RETOUR'
      || n === 'INTERDICTION_RETOUR_TERRITOIRE_FRANCAIS') {
    return 'IRTF';
  }
  if (n === 'IAT' || n === 'INTERDICTION_ADMINISTRATIVE_TERRITOIRE'
      || n === 'INTERDICTION_TERRITOIRE') {
    return 'IAT';
  }
  if (n === 'EXPULSION_MINISTERIELLE' || n === 'EXPULSION_MINISTRE'
      || n === 'EXPULSION_MINISTRE_INTERIEUR') {
    return 'EXPULSION_MINISTERIELLE';
  }
  if (n === 'EXPULSION_SECURITE_ETAT' || n === 'EXPULSION_SURETE_ETAT'
      || n === 'EXPULSION_SECURITE_NATIONALE') {
    return 'EXPULSION_SECURITE_ETAT';
  }
  if (n === 'EXPULSION' || n === 'EXPULSION_PREFECTORALE'
      || n === 'EXPULSION_PREFET' || n === 'EXPULSION_ADMINISTRATIVE') {
    return 'EXPULSION_PREFECTORALE';
  }
  return null;
}

/**
 * Mappe une chaîne IA libre vers l'enum `MotifMenaceCode`.
 * Normalisation idem `mapDispositifFromIa`. Retourne `null` si non reconnu.
 */
export function mapMotifMenaceFromIa(value: string | null | undefined): MotifMenaceCode | null {
  if (!value) return null;
  const n = value.toString().trim().toUpperCase()
    .replace(/[\s\-]+/g, '_')
    .replace(/[ÉÈÊË]/g, 'E')
    .replace(/[ÀÂÄ]/g, 'A')
    .replace(/[ÎÏ]/g, 'I')
    .replace(/[ÔÖ]/g, 'O')
    .replace(/[ÙÛÜ]/g, 'U');

  if (n === 'ORDRE_PUBLIC' || n === 'TROUBLE_ORDRE_PUBLIC'
      || n === 'MENACE_ORDRE_PUBLIC') {
    return 'ORDRE_PUBLIC';
  }
  if (n === 'SECURITE_ETAT' || n === 'SURETE_ETAT'
      || n === 'SECURITE_NATIONALE') {
    return 'SECURITE_ETAT';
  }
  if (n === 'TERRORISME' || n === 'TERRORISTE'
      || n === 'ACTE_TERRORISTE' || n === 'TERROR') {
    return 'TERRORISME';
  }
  if (n === 'RECIDIVE_GRAVE' || n === 'RECIDIVE'
      || n === 'RECIDIVE_LEGALE') {
    return 'RECIDIVE_GRAVE';
  }
  if (n === 'AUTRE' || n === 'OTHER') {
    return 'AUTRE';
  }
  return null;
}

/** Format humain "X jour(s) {TA|CE}" pour l'UI. */
export function formatDelaiRecours(jours: number, juridiction: JuridictionRecours): string {
  const j = jours === 1 ? '1 jour' : `${jours} jours`;
  return `${j} (${juridiction})`;
}
