/**
 * SF-FA-24-08 : modèles TypeScript pour l'outil décisionnel "Réserve
 * héréditaire et action en réduction" (F-FA-24, FR — art. 913 + 914-1
 * + 920-928 Cciv).
 *
 * Contrat figé dans SF-FA-24-07 (backend, mergé PR #672).
 */

/** Qualité du demandeur de l'action en réduction. */
export type QualiteDemandeurReserve =
  | 'HERITIER_RESERVATAIRE_DESCENDANT'
  | 'CONJOINT_SURVIVANT';

/** Verdict de recevabilité de l'action en réduction. */
export type VerdictRecevabiliteReserve =
  | 'RECEVABLE'
  | 'NON_RECEVABLE_PAS_EXCEDENT'
  | 'NON_RECEVABLE_PRESCRIPTION'
  | 'NON_RECEVABLE_QUALITE'
  | 'NON_RECEVABLE';

/** Libellés humains des qualités du demandeur. */
export const QUALITE_DEMANDEUR_RESERVE_LABELS:
  Readonly<Record<QualiteDemandeurReserve, string>> = {
  HERITIER_RESERVATAIRE_DESCENDANT: 'Héritier réservataire (descendant)',
  CONJOINT_SURVIVANT: 'Conjoint survivant',
};

/** Libellés humains du verdict (5 valeurs). */
export const VERDICT_RECEVABILITE_RESERVE_LABELS:
  Readonly<Record<VerdictRecevabiliteReserve, string>> = {
  RECEVABLE: 'Action en réduction recevable',
  NON_RECEVABLE_PAS_EXCEDENT: 'Action non recevable — pas d\'excédent',
  NON_RECEVABLE_PRESCRIPTION: 'Action non recevable — prescrite (5 ans)',
  NON_RECEVABLE_QUALITE: 'Action non recevable — qualité du demandeur',
  NON_RECEVABLE: 'Action non recevable',
};

/** Ligne du tableau visuel du barème (art. 913 + 914-1 Cciv). */
export interface BaremeReserveRow {
  configuration: string;
  quotitePct: number;
  reservePct: number;
  highlighted: boolean;
}

export interface ReserveHereditaireRequest {
  nombreEnfants: number;
  conjointSurvivant: boolean;
  montantSuccession: number;
  montantLibsTotal: number;
  /** Format ISO `YYYY-MM-DD`. */
  dateOuvertureSuccession: string;
  qualiteDuDemandeur: QualiteDemandeurReserve;
}

export interface ReserveHereditaireResponse {
  caseFileId: string;
  nombreEnfants: number;
  conjointSurvivant: boolean;
  montantSuccession: number;
  montantLibsTotal: number;
  dateOuvertureSuccession: string;
  qualiteDuDemandeur: QualiteDemandeurReserve;
  quotiteDisponiblePct: number;
  reservePct: number;
  montantReserveEur: number;
  montantQuotiteDispoEur: number;
  excedentReductibleEur: number;
  actionReductionRecevable: boolean;
  delaiPrescriptionRestantMois: number;
  verdictRecevabilite: VerdictRecevabiliteReserve;
  scoreEligibilite: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: string;
}
