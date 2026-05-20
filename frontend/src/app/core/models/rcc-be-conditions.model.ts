/**
 * SF-207-06b : modèle frontend de l'outil F-207 — RCC BE conditions d'éligibilité
 * (Belgique uniquement, Régime de Chômage avec Complément d'entreprise, ex-prépension).
 *
 * <p>4 régimes parallèles évalués par le calculateur backend :
 *   - GENERAL — CCT 17 art. 3 — âge ≥ 60 ans ET carrière ≥ 40 ans
 *   - METIERS_LOURDS — CCT 17/13 — âge ≥ 58 ans ET carrière ≥ 35 ans ET métier lourd
 *   - LONGUE_CARRIERE — CCT 17 art. 3 (variante) — âge ≥ 59 ans ET carrière ≥ 40 ans ET longueCarriere
 *   - ENTREPRISE_DIFFICULTE — AR 03/05/2007 art. 8 — âge ≥ 60 ans ET entreprise en difficulté
 *
 * <p>Contrat figé dans SF-207-06 backend (PR #1151).
 */

/**
 * Verdict d'éligibilité au RCC. 3 états (cf. backend `RccBeConditionsResult.Verdict`).
 * - ELIGIBLE : au moins un régime matche.
 * - INCERTAIN : aucun régime ne matche, mais âge ≥ 55 ET carrière ≥ 30 (proche des seuils).
 * - NON_ELIGIBLE : sinon.
 */
export type Verdict = 'ELIGIBLE' | 'INCERTAIN' | 'NON_ELIGIBLE';

/**
 * Les 4 régimes parallèles RCC BE (cf. backend `RccBeConditionsRegime`).
 * Priorité : GENERAL > LONGUE_CARRIERE > METIERS_LOURDS > ENTREPRISE_DIFFICULTE.
 */
export type Regime =
  | 'GENERAL'
  | 'METIERS_LOURDS'
  | 'LONGUE_CARRIERE'
  | 'ENTREPRISE_DIFFICULTE';

/**
 * Conditions individuelles non remplies dans le régime LE PLUS PROCHE des seuils
 * (cf. backend `RccBeConditionsCondition`). Vide si verdict ELIGIBLE.
 */
export type Condition =
  | 'AGE_GENERAL_60'
  | 'CARRIERE_40'
  | 'METIER_LOURD_FLAG'
  | 'AGE_METIERS_LOURDS_58'
  | 'CARRIERE_METIERS_LOURDS_35'
  | 'LONGUE_CARRIERE_FLAG'
  | 'ENTREPRISE_DIFFICULTE_FLAG';

export interface RccBeConditionsRequest {
  /** Requis, ISO YYYY-MM-DD — date de naissance du salarié. */
  dateNaissance: string;
  /** Requis, 0-60 — années de carrière professionnelle salariée. */
  anneesCarriereProfessionnelle: number;
  /** Métier lourd reconnu CCT 17/13. */
  metierLourd: boolean;
  /** Flag longue carrière CCT 17 art. 3 (variante). */
  longueCarriere: boolean;
  /** Entreprise reconnue en difficulté par arrêté ministériel (AR 03/05/2007 art. 8). */
  entrepriseEnDifficulte: boolean;
  /**
   * Optionnel — date de licenciement envisagée (ISO YYYY-MM-DD). Défaut = aujourd'hui
   * (Europe/Brussels) côté backend. Utilisée pour le calcul de l'âge au jour
   * du licenciement.
   */
  dateLicenciementEnvisagee?: string | null;
}

export interface RccBeConditionsResponse {
  caseFileId: string;
  // Inputs normalisés (pré-fill UI au reload)
  dateNaissance: string;
  anneesCarriereProfessionnelle: number;
  metierLourd: boolean;
  longueCarriere: boolean;
  entrepriseEnDifficulte: boolean;
  dateLicenciementEnvisagee: string;
  // Outputs calculés
  verdict: Verdict;
  /** Régime retenu (priorité GENERAL > LONGUE_CARRIERE > METIERS_LOURDS > ENTREPRISE_DIFFICULTE). Null si aucun régime ne matche. */
  regimeApplicable: Regime | null;
  /** Tous les régimes qui matchent (ordre = ordre d'évaluation backend). */
  regimesEligibles: Regime[];
  /** Âge en années pleines à la date envisagée (fuseau Europe/Brussels). */
  ageALaDateLicenciement: number;
  /** Carrière saisie par l'avocat (entier ≥ 0). */
  anneesCarriereCalculees: number;
  /** Conditions non remplies dans le régime LE PLUS PROCHE des seuils. Vide si ELIGIBLE. */
  conditionsManquantes: Condition[];
  baseJuridique: string;
  formuleCalcul: string;
  country: 'BELGIQUE';
}

// --------------------------------------------------------------------------
// Humanisation FR — libellés UI (alignés sur les enums backend).
// --------------------------------------------------------------------------

const VERDICT_LABELS: Record<Verdict, string> = {
  ELIGIBLE: 'Éligible au RCC',
  INCERTAIN: 'Éligibilité incertaine',
  NON_ELIGIBLE: 'Non éligible au RCC',
};

const REGIME_LABELS: Record<Regime, string> = {
  GENERAL: 'Régime général (CCT 17)',
  METIERS_LOURDS: 'Métiers lourds (CCT 17/13)',
  LONGUE_CARRIERE: 'Longue carrière (CCT 17 art. 3)',
  ENTREPRISE_DIFFICULTE: 'Entreprise en difficulté (AR 03/05/2007)',
};

const CONDITION_LABELS: Record<Condition, string> = {
  AGE_GENERAL_60: 'Âge ≥ 60 ans (régime général)',
  CARRIERE_40: 'Carrière professionnelle ≥ 40 ans',
  METIER_LOURD_FLAG: 'Reconnaissance de métier lourd (CCT 17/13)',
  AGE_METIERS_LOURDS_58: 'Âge ≥ 58 ans (régime métiers lourds)',
  CARRIERE_METIERS_LOURDS_35: 'Carrière professionnelle ≥ 35 ans (régime métiers lourds)',
  LONGUE_CARRIERE_FLAG: 'Reconnaissance longue carrière',
  ENTREPRISE_DIFFICULTE_FLAG: 'Reconnaissance entreprise en difficulté (arrêté ministériel)',
};

/** Libellé humain FR du verdict (badge + en-tête résultat). */
export function humanizeVerdict(v: Verdict): string {
  return VERDICT_LABELS[v] ?? v;
}

/** Libellé humain FR d'un régime RCC (régime applicable + régimes cumulés). */
export function humanizeRegime(r: Regime): string {
  return REGIME_LABELS[r] ?? r;
}

/** Libellé humain FR d'une condition (liste « conditions manquantes »). */
export function humanizeCondition(c: Condition): string {
  return CONDITION_LABELS[c] ?? c;
}
