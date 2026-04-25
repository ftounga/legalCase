/**
 * SF-DT-18-02 : modèles TypeScript pour l'outil décisionnel
 * "Indemnité de fin de mission intérim" (art. L.1251-32 Code du travail —
 * IFM 10 % du total des rémunérations brutes perçues pendant la mission ;
 * art. L.1251-33 cas d'exclusion).
 *
 * Contrat API figé dans SF-DT-18-01 (backend, parallèle).
 * Endpoint : POST/GET /api/v1/case-files/{caseFileId}/fin-mission-interim
 */

/**
 * Cas d'exclusion de l'art. L.1251-33 du Code du travail — situations dans
 * lesquelles l'indemnité de fin de mission n'est pas due.
 */
export type MotifExclusionInterim =
  | 'CONTRAT_INDETERMINEE_PROPOSE'
  | 'RUPTURE_ANTICIPEE_SALARIE'
  | 'FAUTE_GRAVE'
  | 'FORCE_MAJEURE'
  | 'MISSION_PEPINIERE_QUALIFIANTE'
  | 'INTERIMAIRE_REFUS_PROPOSITION_CDI';

export interface MotifExclusionOption {
  code: MotifExclusionInterim;
  label: string;
  description: string;
}

/**
 * 6 motifs d'exclusion L.1251-33 pour le <mat-select>.
 * Ordre : situations à l'initiative de l'employeur / pépinière (1, 5),
 * puis situations à l'initiative ou imputables au salarié (2, 3, 6),
 * puis force majeure (4).
 */
export const MOTIF_EXCLUSION_OPTIONS: MotifExclusionOption[] = [
  {
    code: 'CONTRAT_INDETERMINEE_PROPOSE',
    label: 'CDI proposé par l\'utilisateur',
    description: 'CDI proposé immédiatement après la mission par l\'entreprise utilisatrice (L.1251-33)',
  },
  {
    code: 'INTERIMAIRE_REFUS_PROPOSITION_CDI',
    label: 'CDI proposé refusé par l\'intérimaire',
    description: 'L\'intérimaire refuse une proposition de CDI pour le même emploi (L.1251-33)',
  },
  {
    code: 'RUPTURE_ANTICIPEE_SALARIE',
    label: 'Rupture anticipée à l\'initiative du salarié',
    description: 'Rupture anticipée par l\'intérimaire (L.1251-33)',
  },
  {
    code: 'FAUTE_GRAVE',
    label: 'Faute grave de l\'intérimaire',
    description: 'Rupture pour faute grave imputable au salarié (L.1251-33)',
  },
  {
    code: 'FORCE_MAJEURE',
    label: 'Force majeure',
    description: 'Rupture pour cas de force majeure (L.1251-33)',
  },
  {
    code: 'MISSION_PEPINIERE_QUALIFIANTE',
    label: 'Mission pépinière qualifiante',
    description: 'Mission s\'inscrivant dans un parcours qualifiant agréé (L.1251-33)',
  },
];

export interface FinMissionInterimRequest {
  /** Total des rémunérations brutes perçues pendant la mission (> 0). */
  totalRemunerationsBrutesEur: number;
  /** Durée de la mission en jours (> 0). */
  dureeMissionJours: number;
  /** Motif d'exclusion L.1251-33 — nullable. */
  motifExclusion?: MotifExclusionInterim | null;
  /** Date de fin de mission (format ISO YYYY-MM-DD). */
  dateFinMission: string;
}

export interface FinMissionInterimResponse {
  caseFileId: string;
  totalRemunerationsBrutesEur: number;
  dureeMissionJours: number;
  motifExclusion: MotifExclusionInterim | null;
  dateFinMission: string;
  /** Taux applique : 0.10 (10 %) si non exclue, 0 si exclue. */
  tauxApplique: number;
  montantIndemniteEur: number;
  exclusionRetenue: boolean;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}
