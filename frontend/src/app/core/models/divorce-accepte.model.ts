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
  /**
   * SF-155-20 : valeur vénale du bien immobilier principal (€) — utilisée
   * par l'outil F-FA-05 (partage immobilier) pour pré-remplir le champ
   * "Valeur vénale". Optionnel ; absent si le pipeline IA ne l'a pas extrait.
   */
  valeurImmeuble?: number | null;
  /**
   * SF-155-20 : capital restant dû du prêt hypothécaire associé (€).
   * Pré-remplit le champ "Capital restant dû" de l'outil F-FA-05.
   */
  capitalRestantDu?: number | null;
  /**
   * SF-FA-11-02 : date de séparation effective (ISO YYYY-MM-DD) — pré-fill
   * pour l'outil F-FA-11 désunion irrémédiable BE (art. 229 CC).
   */
  dateSeparation?: string | null;
  /**
   * SF-FA-11-02 : séparation consentue par les 2 époux — pré-fill pour
   * l'outil F-FA-11 désunion irrémédiable BE.
   */
  separationConsentue?: boolean | null;
  /**
   * SF-FA-15-02 : régime matrimonial détecté par l'IA — pré-fill pour
   * l'outil F-FA-15 récompenses (art. 1437/1469 Cciv). Valeurs attendues :
   * `COMMUNAUTE_LEGALE` / `PARTICIPATION_AUX_ACQUETS` / `COMMUNAUTE_UNIVERSELLE` /
   * `SEPARATION_BIENS` (cette dernière exclue de l'UI car récompenses N/A).
   * Optionnel ; absent si le pipeline IA ne l'a pas extrait — graceful no-op.
   */
  regimeMatrimonialDetecte?: string | null;
}
