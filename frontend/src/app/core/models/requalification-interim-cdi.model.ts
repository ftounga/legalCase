/**
 * SF-DT-23-02 : modèles TypeScript pour l'outil décisionnel
 * "Requalification intérim → CDI" (art. L.1251-40, L.1251-1 à L.1251-12,
 *  L.1251-32 et L.1251-41 Code du travail, FR uniquement).
 *
 * Contrat API figé dans SF-DT-23-01 (backend).
 * Endpoint : POST/GET /api/v1/case-files/{caseFileId}/requalification-interim-cdi
 *
 * Pattern jumeau direct : F-DT-22 (requalification CDD → CDI). Différences :
 *   - 6 motifs (vs 7 — pas de CONTRAT_VENDANGE, pas d'INSERTION_PROFESSIONNELLE,
 *     mais MISSION_PEPINIERE spécifique intérim)
 *   - chaque mission contient l'`entrepriseUtilisatrice` (relation triangulaire)
 *   - toggle `memeEntrepriseUtilisatrice` (faisceau jurisprudentiel Cass. soc.)
 *   - indemnité de fin de mission 10 % L.1251-32 (au lieu de précarité L.1243-8)
 */

/**
 * Motif de recours à l'intérim invoqué par l'entreprise utilisatrice
 * (art. L.1251-6). 5 valeurs canoniques + AUTRE.
 */
export type MotifInterimInvoque =
  | 'ACCROISSEMENT_TEMPORAIRE'
  | 'REMPLACEMENT_SALARIE'
  | 'EMPLOI_SAISONNIER'
  | 'EMPLOI_USAGE'
  | 'MISSION_PEPINIERE'
  | 'AUTRE';

/**
 * Type de motif interdit (art. L.1251-5, L.1251-10).
 */
export type MotifInterditTypeInterim =
  | 'EMPLOI_PERMANENT'
  | 'REMPLACEMENT_GREVISTE'
  | 'TRAVAUX_DANGEREUX'
  | 'AUTRE';

/**
 * Verdict de probabilité de requalification produit par le scoring backend.
 */
export type VerdictProbabiliteRequalificationInterim = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

/**
 * Élément d'une succession de missions d'intérim. Chaque mission est
 * effectuée pour une entreprise utilisatrice donnée (relation triangulaire
 * ETT / EU / salarié — cf. Cass. soc.).
 */
export interface MissionInterim {
  /** ISO YYYY-MM-DD. */
  dateDebut: string;
  /** ISO YYYY-MM-DD. */
  dateFin: string;
  /** Texte libre (motif de recours). */
  motif: string;
  /** Raison sociale de l'entreprise utilisatrice. */
  entrepriseUtilisatrice: string;
}

export interface RequalificationInterimCdiRequest {
  motifInterimInvoque: MotifInterimInvoque;
  motifInterdit: boolean;
  motifInterditType?: MotifInterditTypeInterim | null;
  successionMissions: MissionInterim[];
  delaiCarenceRespecte: boolean;
  dureeMissionsTotaleMois: number;
  salaireMensuelBrutEur: number;
  /** ISO YYYY-MM-DD. */
  dateFinDerniereMission: string;
  memeEntrepriseUtilisatrice: boolean;
}

export interface RequalificationInterimCdiResponse {
  caseFileId: string;
  motifInterimInvoque: MotifInterimInvoque;
  motifInterdit: boolean;
  motifInterditType: MotifInterditTypeInterim | null;
  successionMissions: MissionInterim[];
  delaiCarenceRespecte: boolean;
  dureeMissionsTotaleMois: number;
  salaireMensuelBrutEur: number;
  dateFinDerniereMission: string;
  memeEntrepriseUtilisatrice: boolean;
  scoreRequalification: number;
  verdictProbabiliteRequalification: VerdictProbabiliteRequalificationInterim;
  indemniteRequalificationEur: number;
  indemniteFinMissionInterimEur: number;
  totalDommagesIndemniteEur: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}

export interface MotifInterimInvoqueOption {
  code: MotifInterimInvoque;
  label: string;
}

export interface MotifInterditTypeInterimOption {
  code: MotifInterditTypeInterim;
  label: string;
}

/**
 * 6 motifs intérim valides art. L.1251-6.
 */
export const MOTIF_INTERIM_INVOQUE_OPTIONS: MotifInterimInvoqueOption[] = [
  { code: 'ACCROISSEMENT_TEMPORAIRE', label: "Accroissement temporaire d'activité" },
  { code: 'REMPLACEMENT_SALARIE', label: 'Remplacement de salarié absent' },
  { code: 'EMPLOI_SAISONNIER', label: 'Emploi saisonnier' },
  { code: 'EMPLOI_USAGE', label: "Emploi par nature temporaire (usage)" },
  { code: 'MISSION_PEPINIERE', label: 'Mission pépinière (recrutement à l\'issue)' },
  { code: 'AUTRE', label: 'Autre motif' },
];

/**
 * 4 types de motif interdit art. L.1251-5 / L.1251-10.
 */
export const MOTIF_INTERDIT_TYPE_INTERIM_OPTIONS: MotifInterditTypeInterimOption[] = [
  { code: 'EMPLOI_PERMANENT', label: "Pourvoir un emploi lié à l'activité normale et permanente" },
  { code: 'REMPLACEMENT_GREVISTE', label: 'Remplacement de salarié gréviste' },
  { code: 'TRAVAUX_DANGEREUX', label: 'Travaux particulièrement dangereux (L.1251-10)' },
  { code: 'AUTRE', label: 'Autre interdiction' },
];

export function motifInterimInvoqueLabel(code: MotifInterimInvoque): string {
  return MOTIF_INTERIM_INVOQUE_OPTIONS.find((o) => o.code === code)?.label ?? code;
}

export function motifInterditTypeInterimLabel(code: MotifInterditTypeInterim): string {
  return MOTIF_INTERDIT_TYPE_INTERIM_OPTIONS.find((o) => o.code === code)?.label ?? code;
}

export function verdictProbabiliteInterimLabel(v: VerdictProbabiliteRequalificationInterim): string {
  switch (v) {
    case 'ELEVEE': return 'Probabilité élevée';
    case 'MOYENNE': return 'Probabilité moyenne';
    case 'FAIBLE': return 'Probabilité faible';
  }
}
