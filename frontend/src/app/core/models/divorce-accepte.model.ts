/**
 * SF-FA-10-02 : modèles TypeScript pour l'outil "Divorce accepté"
 * (art. 233-234 Cciv + 1123 CPC). FRANCE uniquement.
 *
 * Backend : `DivorceAccepteRequest` / `DivorceAccepteResponse` (PR #514).
 */

export type DivorceAccepteVerdict = 'ELEVEE' | 'FAIBLE';

export interface DivorceAccepteRequest {
  acceptationPrincipeSignee: boolean;
  /** ISO date YYYY-MM-DD (optionnel — si null le PV n'a pas encore été signé). */
  dateAcceptationPV?: string | null;
  dureeMariageAnnees: number;
  revenusAnnuelsEpoux1Eur: number;
  revenusAnnuelsEpoux2Eur: number;
  patrimoineCommun: boolean;
  /** ISO date YYYY-MM-DD (optionnel — date d'assignation devant le JAF). */
  dateAssignation?: string | null;
}

export interface DivorceAccepteResponse {
  caseFileId: string;
  acceptationPrincipeSignee: boolean;
  dateAcceptationPV: string | null;
  dureeMariageAnnees: number;
  revenusAnnuelsEpoux1Eur: number;
  revenusAnnuelsEpoux2Eur: number;
  patrimoineCommun: boolean;
  dateAssignation: string | null;
  country: 'FRANCE';
  acceptationValide: boolean;
  ordrePublic: boolean;
  eligibilite: boolean;
  scoreGlobal: number;
  verdictEligibilite: DivorceAccepteVerdict;
  delaiProcedureMoisPrevisionnel: number;
  prestationCompensatoireFourchetteMin: number;
  prestationCompensatoireFourchetteMax: number;
  criteresNonRemplis: string[];
  formule: string;
  baseJuridique: string;
  messages: string[];
}

/**
 * SF-FA-10-02 : structure minimale frontend pour le pré-fill IA des outils
 * famille (divorce accepté, et plus tard altération / faute / etc.).
 *
 * Tous les champs sont optionnels et peuvent être absents — les composants
 * doivent être no-op gracieux. Cette interface est volontairement gardée
 * frontend-only tant que le backend n'expose pas un type équivalent
 * `FamilleExtractedData` dans `CaseAnalysisResult`.
 */
export interface FamilleExtractedData {
  /** Durée du mariage en années entières (depuis la date du mariage). */
  dureeMariageAnnees?: number | null;
  /** Revenus annuels bruts époux 1 (€). */
  revenusAnnuelsEpoux1Eur?: number | null;
  /** Revenus annuels bruts époux 2 (€). */
  revenusAnnuelsEpoux2Eur?: number | null;
  /** Régime matrimonial : true si communauté ou participation aux acquêts. */
  patrimoineCommun?: boolean | null;
  /** Date de signature du PV d'acceptation (ISO YYYY-MM-DD). */
  dateAcceptationPV?: string | null;
}
