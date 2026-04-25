/**
 * SF-DT-30-02 : types frontend pour l'outil décisionnel "Protection des
 * représentants du personnel" (F-DT-30). FR uniquement (art. L.2411-1 +
 * L.2411-3 + L.2411-22 + L.2422-1 + R.2422-1 Code du travail).
 *
 * Contrat figé dans SF-DT-30-01 (backend, mergé PR #631).
 */

/** Statut protégé du salarié (L.2411-1 et s.). 9 valeurs alignées backend. */
export type StatutProtege =
  | 'MEMBRE_CSE_TITULAIRE'
  | 'MEMBRE_CSE_SUPPLEANT'
  | 'DELEGUE_SYNDICAL'
  | 'REPRESENTANT_SECTION_SYNDICALE'
  | 'CONSEILLER_PRUDHOMMES'
  | 'CONSEILLER_SALARIE'
  | 'DEFENSEUR_SYNDICAL'
  | 'MEDECIN_TRAVAIL'
  | 'MEMBRE_CHSCT_HISTORIQUE';

/** Procédure suivie par l'employeur. 4 valeurs alignées backend. */
export type ProcedureSuivie =
  | 'AUTORISATION_OBTENUE'
  | 'AUTORISATION_REFUSEE'
  | 'EN_COURS_INSTRUCTION'
  | 'AUCUNE_DEMANDE';

/** Motif de licenciement (FR standard). 5 valeurs alignées backend. */
export type MotifLicenciement =
  | 'FAUTE_GRAVE'
  | 'INSUFFISANCE_PRO'
  | 'ECONOMIQUE'
  | 'INAPTITUDE'
  | 'AUTRE';

/** Verdict de légalité (scoring niveau 5). */
export type VerdictLegalite = 'VALIDE' | 'CONTESTABLE' | 'NUL';

/** Critères de régularité procédurale. */
export type CritereRegularite =
  | 'STATUT_PROTEGE_VALIDE'
  | 'PROCEDURE_AUTORISATION_DEMANDEE'
  | 'AUTORISATION_OBTENUE'
  | 'LICENCIEMENT_HORS_PROCEDURE_EN_COURS';

/** Libellés humains pour mat-radio statut protégé. */
export const STATUT_PROTEGE_LABELS:
  ReadonlyArray<{ code: StatutProtege; label: string }> = [
  { code: 'MEMBRE_CSE_TITULAIRE', label: 'Membre CSE titulaire (L.2411-1)' },
  { code: 'MEMBRE_CSE_SUPPLEANT', label: 'Membre CSE suppléant (L.2411-1)' },
  { code: 'DELEGUE_SYNDICAL', label: 'Délégué syndical (L.2411-3)' },
  { code: 'REPRESENTANT_SECTION_SYNDICALE', label: 'Représentant de section syndicale (L.2411-4)' },
  { code: 'CONSEILLER_PRUDHOMMES', label: 'Conseiller prud\'hommes (L.2411-22)' },
  { code: 'CONSEILLER_SALARIE', label: 'Conseiller du salarié (L.2411-21)' },
  { code: 'DEFENSEUR_SYNDICAL', label: 'Défenseur syndical (L.2411-24)' },
  { code: 'MEDECIN_TRAVAIL', label: 'Médecin du travail (L.2411-5)' },
  { code: 'MEMBRE_CHSCT_HISTORIQUE', label: 'Membre CHSCT (mandat résiduel)' },
];

/** Libellés humains pour mat-radio procédure suivie. */
export const PROCEDURE_SUIVIE_LABELS:
  ReadonlyArray<{ code: ProcedureSuivie; label: string }> = [
  { code: 'AUTORISATION_OBTENUE', label: 'Autorisation obtenue (avis IT favorable)' },
  { code: 'AUTORISATION_REFUSEE', label: 'Autorisation refusée (avis IT défavorable)' },
  { code: 'EN_COURS_INSTRUCTION', label: 'Procédure en cours d\'instruction' },
  { code: 'AUCUNE_DEMANDE', label: 'Aucune demande déposée' },
];

/** Libellés humains pour mat-radio motif de licenciement. */
export const MOTIF_LICENCIEMENT_LABELS:
  ReadonlyArray<{ code: MotifLicenciement; label: string }> = [
  { code: 'FAUTE_GRAVE', label: 'Faute grave' },
  { code: 'INSUFFISANCE_PRO', label: 'Insuffisance professionnelle' },
  { code: 'ECONOMIQUE', label: 'Motif économique' },
  { code: 'INAPTITUDE', label: 'Inaptitude' },
  { code: 'AUTRE', label: 'Autre motif' },
];

/** Libellés humains pour les codes critère de régularité. */
export const CRITERE_REGULARITE_LABELS: Record<CritereRegularite, string> = {
  STATUT_PROTEGE_VALIDE: 'Statut protégé reconnu (L.2411-1)',
  PROCEDURE_AUTORISATION_DEMANDEE: 'Procédure d\'autorisation déposée',
  AUTORISATION_OBTENUE: 'Autorisation IT obtenue',
  LICENCIEMENT_HORS_PROCEDURE_EN_COURS: 'Licenciement prononcé hors instruction en cours',
};

export interface ProtectionRpRequest {
  statutProtege: StatutProtege;
  /** YYYY-MM-DD. */
  dateExpirationMandat: string;
  /** YYYY-MM-DD. */
  datePresumeeRupture: string;
  procedureSuivie: ProcedureSuivie;
  motifLicenciement: MotifLicenciement;
  /** Optionnel — pour estimer indemnités si verdict NUL. */
  salaireMensuelBrutEur?: number;
}

export interface ProtectionRpResponse {
  caseFileId: string;
  salarieEncoreProtege: boolean;
  scoreConformite: number;
  verdictLegalite: VerdictLegalite;
  criteresRemplis: CritereRegularite[];
  criteresManquants: CritereRegularite[];
  /** Indemnité forfaitaire ≥ 6 × salaire si verdict NUL (L.2422-1). */
  indemniteForfaitaireMinEur: number;
  /** Estimation salaires éviction si NUL. */
  salaireEvictionPotentielEur: number;
  /** Délai de contestation (R.2422-1) — 60 jours. */
  delaiContestationJours: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: string;
}

/**
 * Mappe une chaîne IA libre (`aiData.motifLicenciement`) vers l'enum backend.
 * Normalisation insensible à la casse, accents, tirets/underscores. Retourne
 * `null` si non reconnu (no-op gracieux pour le pré-fill).
 */
export function mapMotifLicenciementFromIa(value: string | null | undefined): MotifLicenciement | null {
  if (!value) return null;
  const normalized = value.toString().trim().toUpperCase()
    .replace(/[\s\-]+/g, '_')
    .replace(/[ÉÈÊË]/g, 'E')
    .replace(/[ÀÂÄ]/g, 'A')
    .replace(/[ÎÏ]/g, 'I')
    .replace(/[ÔÖ]/g, 'O')
    .replace(/[ÙÛÜ]/g, 'U');
  switch (normalized) {
    case 'FAUTE_GRAVE':
    case 'FAUTE_LOURDE':
      return 'FAUTE_GRAVE';
    case 'INSUFFISANCE_PRO':
    case 'INSUFFISANCE_PROFESSIONNELLE':
    case 'INSUFFISANCE':
      return 'INSUFFISANCE_PRO';
    case 'ECONOMIQUE':
    case 'MOTIF_ECONOMIQUE':
      return 'ECONOMIQUE';
    case 'INAPTITUDE':
    case 'INAPTITUDE_MEDICALE':
      return 'INAPTITUDE';
    case 'AUTRE':
    case 'AUTRES':
      return 'AUTRE';
    default:
      return null;
  }
}
