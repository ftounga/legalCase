/**
 * SF-FA-24-10 : modèles TypeScript pour l'outil décisionnel "Partage
 * successoral" (F-FA-24). FR uniquement — art. 815-840 Cciv + 1364 CPC.
 *
 * Contrat figé dans SF-FA-24-09 (backend, mergé PR #680).
 */

/** Modalité de partage demandée par l'avocat (et recommandée par l'outil). */
export type ModePartage =
  | 'PARTAGE_AMIABLE'
  | 'PARTAGE_JUDICIAIRE'
  | 'PARTAGE_PARTIEL';

/** Verdict de recevabilité. */
export type VerdictRecevabilitePartage = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

/** Libellés humains des modes (radio + chip). */
export const MODE_PARTAGE_LABELS: Readonly<Record<ModePartage, string>> = {
  PARTAGE_AMIABLE: 'Partage amiable (art. 835 Cciv)',
  PARTAGE_JUDICIAIRE: 'Partage judiciaire (art. 840 + 1364 CPC)',
  PARTAGE_PARTIEL: 'Partage partiel (art. 838 Cciv)',
};

/** Libellés humains du verdict. */
export const VERDICT_RECEVABILITE_PARTAGE_LABELS:
    Readonly<Record<VerdictRecevabilitePartage, string>> = {
  ELEVEE: 'Recevabilité élevée',
  MOYENNE: 'Recevabilité moyenne',
  FAIBLE: 'Recevabilité faible',
};

/**
 * Requête d'analyse — alignée sur le record backend
 * `PartageSuccessoralRequest`.
 */
export interface PartageSuccessoralRequest {
  modePartageDemande: ModePartage;
  nombreCoheritiers: number;
  consentementsTous: boolean;
  presenceImmeubles: boolean;
  accordsValuation: boolean;
  desaccordPersistant: boolean;
  dateDeces: string; // ISO YYYY-MM-DD
  valeurMasseEur: number | null;
}

/** Réponse de l'endpoint d'analyse. */
export interface PartageSuccessoralResponse {
  caseFileId: string;
  verdictRecevabilite: VerdictRecevabilitePartage;
  modeRecommande: ModePartage;
  basculeMode: boolean;
  scoreEligibilite: number;
  delaiInstructionMois: number;
  fraisEstimesPct: number;
  fraisEstimesEur: number;
  risqueLicitation: boolean;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: string;
}
