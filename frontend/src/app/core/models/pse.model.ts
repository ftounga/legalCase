/**
 * SF-DT-14-02 : types frontend pour l'outil décisionnel "PSE — critères de
 * validité" (F-DT-14). FR uniquement (art. L.1233-24-1 + L.1233-30 +
 * L.1233-57-2 + L.1233-61 + L.1235-7-1 Code du travail).
 *
 * Contrat figé dans SF-DT-14-01 (backend, mergé PR #623).
 */

/** Mode d'adoption du PSE (L.1233-24-1). */
export type ModeAdoption = 'ACCORD_COLLECTIF_MAJORITAIRE' | 'DOCUMENT_UNILATERAL';

/** Avis du CSE (L.1233-30). */
export type AvisCse = 'FAVORABLE' | 'DEFAVORABLE' | 'NON_CONSULTE';

/** Statut DREETS (L.1233-57-2). */
export type StatutDreets = 'VALIDE' | 'HOMOLOGUE' | 'REFUS' | 'EN_COURS';

/** Mesures du contenu PSE (L.1233-61). */
export type ContenuMesure =
  | 'RECLASSEMENT_INTERNE'
  | 'RECLASSEMENT_EXTERNE'
  | 'FORMATION'
  | 'AIDE_CREATION'
  | 'INDEMNITES_SUPRA'
  | 'CONGE_RECLASSEMENT'
  | 'CELLULE_RECLASSEMENT'
  | 'AUTRE';

/** Verdict global de validité du PSE (scoring niveau 5). */
export type VerdictValidite = 'VALIDE' | 'CONTESTABLE' | 'NUL';

/** Critères de validité évalués par le calculator (L.1233-30 / 57-2 / 61). */
export type CritereValidite =
  | 'CSE_CONSULTE'
  | 'DREETS_VALIDE_OU_HOMOLOGUE'
  | 'CONTENU_RECLASSEMENT'
  | 'MODE_DREETS_COHERENT'
  | 'MESURES_MULTIPLES';

/** Libellé humain pour mat-radio mode adoption. */
export const MODE_ADOPTION_LABELS: ReadonlyArray<{ code: ModeAdoption; label: string }> = [
  { code: 'ACCORD_COLLECTIF_MAJORITAIRE', label: 'Accord collectif majoritaire (DREETS valide)' },
  { code: 'DOCUMENT_UNILATERAL', label: 'Document unilatéral employeur (DREETS homologue)' },
];

/** Libellé humain pour mat-radio avis CSE. */
export const AVIS_CSE_LABELS: ReadonlyArray<{ code: AvisCse; label: string }> = [
  { code: 'FAVORABLE', label: 'Favorable' },
  { code: 'DEFAVORABLE', label: 'Défavorable' },
  { code: 'NON_CONSULTE', label: 'Non consulté (vice de procédure)' },
];

/** Libellé humain pour mat-radio statut DREETS. */
export const STATUT_DREETS_LABELS: ReadonlyArray<{ code: StatutDreets; label: string }> = [
  { code: 'VALIDE', label: 'Validation accord majoritaire' },
  { code: 'HOMOLOGUE', label: 'Homologation document unilatéral' },
  { code: 'REFUS', label: 'Refus de validation/homologation' },
  { code: 'EN_COURS', label: 'En cours d\'instruction' },
];

/** Libellé humain pour checkbox contenu mesures. */
export const CONTENU_MESURE_LABELS: ReadonlyArray<{ code: ContenuMesure; label: string }> = [
  { code: 'RECLASSEMENT_INTERNE', label: 'Reclassement interne' },
  { code: 'RECLASSEMENT_EXTERNE', label: 'Reclassement externe' },
  { code: 'FORMATION', label: 'Formation' },
  { code: 'AIDE_CREATION', label: 'Aide à la création d\'entreprise' },
  { code: 'INDEMNITES_SUPRA', label: 'Indemnités supra-légales' },
  { code: 'CONGE_RECLASSEMENT', label: 'Congé de reclassement' },
  { code: 'CELLULE_RECLASSEMENT', label: 'Cellule de reclassement' },
  { code: 'AUTRE', label: 'Autre mesure' },
];

/** Libellé humain pour les codes critère de validité. */
export const CRITERE_VALIDITE_LABELS: Record<CritereValidite, string> = {
  CSE_CONSULTE: 'CSE consulté (L.1233-30)',
  DREETS_VALIDE_OU_HOMOLOGUE: 'DREETS validé ou homologué (L.1233-57-2)',
  CONTENU_RECLASSEMENT: 'Reclassement interne prévu (L.1233-61)',
  MODE_DREETS_COHERENT: 'Mode adoption cohérent avec DREETS (L.1233-57-2)',
  MESURES_MULTIPLES: 'Au moins 2 mesures distinctes (L.1233-61)',
};

export interface PseRequest {
  /** Effectif de l'entreprise (≥ 0). */
  tailleEntrepriseSalaries: number;
  /** Nombre de licenciements économiques envisagés (≥ 0). */
  nombreLicenciementsEnvisages: number;
  /** Période de référence en jours (typiquement 30). */
  periodeJours: number;
  modeAdoption: ModeAdoption;
  csaeConsulteAvis: AvisCse;
  dreetsStatut: StatutDreets;
  /** Date de notification DREETS (YYYY-MM-DD, optionnelle). */
  dateNotificationDreets?: string;
  /** Mesures du contenu PSE (peut être vide). */
  contenuMesures: ContenuMesure[];
  /** Date du projet de licenciement (YYYY-MM-DD). */
  dateProjet: string;
}

export interface PseResponse {
  caseFileId: string;
  /** Déclenchement (L.1233-24-1) — true si seuils atteints. */
  pseRequis: boolean;
  /** Score de conformité 0..100. */
  scoreConformite: number;
  verdictValidite: VerdictValidite;
  criteresRemplis: CritereValidite[];
  criteresManquants: CritereValidite[];
  /** Délai de contestation (L.1235-7-1) — toujours 60 jours. */
  delaiContestationJours: number;
  /** Base juridique (texte JetBrains Mono italique). */
  baseJuridique: string;
  /** Formule (texte JetBrains Mono). */
  formule: string;
  /** Messages explicatifs (rendus via LegalCitationsPipe). */
  messages: string[];
  /** Pays du workspace ('FRANCE' attendu). */
  country: string;
}
