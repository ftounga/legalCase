/**
 * SF-FA-08-02 : modèles TypeScript pour l'outil décisionnel
 * "Divorce pour altération définitive du lien conjugal" (F-FA-08).
 * Contrat figé par SF-FA-08-01 (backend mergé PR #513).
 */

export type DivorceAlterationVerdict = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

export interface DivorceAlterationRequest {
  /** Date de cessation effective de la communauté de vie (ISO YYYY-MM-DD). */
  dateCessationVieCommune: string;
  /** Preuves documentaires de séparation disponibles. */
  preuvesSeparationDocumentaires: boolean;
  /** Tentatives de réconciliation signalées (signal contre la cessation définitive). */
  tentativesReconciliation: boolean;
  /** Durée du mariage en années (≥ 0). */
  dureeMariageAnnees: number;
  /** Revenus annuels époux 1 en euros (≥ 0). */
  revenusAnnuelsEpoux1Eur: number;
  /** Revenus annuels époux 2 en euros (≥ 0). */
  revenusAnnuelsEpoux2Eur: number;
  /** Patrimoine commun significatif (oriente vers liquidation concomitante). */
  patrimoineCommunSignificatif: boolean;
  /** Date d'assignation (ISO YYYY-MM-DD). Optionnelle — calcul du délai 1 an. */
  dateAssignation?: string | null;
}

export interface DivorceAlterationResponse {
  caseFileId: string;
  dateCessationVieCommune: string;
  preuvesSeparationDocumentaires: boolean;
  tentativesReconciliation: boolean;
  dureeMariageAnnees: number;
  revenusAnnuelsEpoux1Eur: number;
  revenusAnnuelsEpoux2Eur: number;
  patrimoineCommunSignificatif: boolean;
  dateAssignation: string | null;
  country: 'FRANCE';
  /** Durée écoulée entre cessation et assignation (ou aujourd'hui), en années. */
  dureeSeparationAnnees: number;
  /** ≥ 1 an (art. 238 Cciv après loi 23/03/2019). */
  delaiObjectifOk: boolean;
  /** !tentativesReconciliation. */
  absencePreuveReconciliation: boolean;
  /** delaiObjectifOk && preuvesSeparationDocumentaires && absencePreuveReconciliation. */
  conditionsReunies: boolean;
  /** Score pondéré 0-100. */
  scoreGlobal: number;
  verdictProbabilite: DivorceAlterationVerdict;
  /** Liste lisible des critères non remplis. */
  criteresNonRemplis: string[];
  /** Estimation prestation compensatoire art. 270 Cciv (min). */
  prestationCompensatoireFourchetteMin: number;
  /** Estimation prestation compensatoire art. 270 Cciv (max). */
  prestationCompensatoireFourchetteMax: number;
  formule: string;
  baseJuridique: string;
  messages: string[];
}

/**
 * SF-FA-08-02 : interface locale pour `aiData` du composant. Les types Famille
 * extraits par le pipeline IA n'existent pas encore dans `case-analysis.model.ts`
 * (out-of-scope ici). Tout champ inconnu de l'objet `aiData` est ignoré
 * silencieusement par le composant. Sera remplacée par `FamilleExtractedData`
 * partagée quand le pipeline IA Famille sera étendu.
 */
export interface FamilleAiData {
  /** Date de cessation détectée par l'IA (ISO YYYY-MM-DD). */
  dateCessationVieCommune?: string | null;
  /** Durée du mariage détectée par l'IA. */
  dureeMariageAnnees?: number | null;
  /** Revenus annuels époux 1 détectés. */
  revenusAnnuelsEpoux1Eur?: number | null;
  /** Revenus annuels époux 2 détectés. */
  revenusAnnuelsEpoux2Eur?: number | null;
  /** Patrimoine commun significatif détecté. */
  patrimoineCommunSignificatif?: boolean | null;
  /**
   * SF-246-27 : date de l'assignation en divorce (art. 56 CPC), ISO YYYY-MM-DD.
   * Source backend réelle : `protection_divorce_detection_v2.date_assignation_divorce`.
   */
  dateAssignationDivorce?: string | null;
}
