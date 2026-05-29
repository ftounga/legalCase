/**
 * SF-215-17 / SF-215-18 — modèles miroirs du contrat API (backend) pour l'outil
 * décisionnel « Annexe 13quinquies OQT + interdiction d'entrée (BE) »
 * (F-IM-33-annexe13quinquies-ie-be).
 *
 * BELGIQUE uniquement — l'annexe 13quinquies est un ordre de quitter le territoire
 * (OQT) ASSORTI d'une interdiction d'entrée (IE) sur le territoire Schengen
 * (Loi du 15/12/1980, art. 74/11 — durée 3 / 5 / 8 ans selon le motif). L'outil
 * calcule : durée de l'IE, date de fin, date de levée précoce possible, et le
 * délai du recours en annulation devant le CCE (Conseil du Contentieux des
 * Étrangers — 30 jours calendaires, art. 39/2 §2).
 */

/** Motif de l'interdiction d'entrée — whitelist stricte (miroir backend). */
export type MotifInterdictionEntree =
  | 'SEJOUR_IRREGULIER'
  | 'MENACE_ORDRE_PUBLIC'
  | 'RAISONS_SECURITE_NATIONALE'
  | 'ATTEINTE_INTERET_UE'
  | 'DECISION_JUDICIAIRE';

/**
 * Statut du recours en annulation contre l'annexe 13quinquies :
 *  - DISPONIBLE : délai ouvert, marge confortable (vert)
 *  - URGENT     : délai ouvert mais proche de l'échéance (orange) → lien F-IM-31
 *  - EXPIRE     : délai dépassé (rouge)
 *  - FORME      : un recours a déjà été formé (bleu info)
 */
export type Annexe13quinquiesStatutRecours =
  | 'DISPONIBLE'
  | 'URGENT'
  | 'EXPIRE'
  | 'FORME';

export interface Annexe13quinquiesBeRequest {
  /** Date de notification de l'annexe 13quinquies (ISO yyyy-MM-dd). */
  dateNotificationAnnexe: string;
  motifInterdictionEntree: MotifInterdictionEntree;
  /** L'intéressé a-t-il déjà séjourné régulièrement en Belgique ? */
  precedentSejour: boolean;
  /** Un recours en annulation a-t-il déjà été formé ? */
  recoursForme: boolean;
  /** Date de dépôt du recours (ISO yyyy-MM-dd) — requis si recoursForme=true. */
  dateRecours?: string | null;
}

export interface Annexe13quinquiesBeResponse {
  caseFileId: string;
  dateNotificationAnnexe: string;
  motifInterdictionEntree: string;
  precedentSejour: boolean;
  recoursForme: boolean;
  dateRecours?: string | null;
  /** Durée de l'interdiction d'entrée en ANNÉES (3 / 5 / 8). */
  dureeInterdiction: number;
  /** Date de fin de l'interdiction d'entrée (ISO yyyy-MM-dd). */
  dateFinInterdiction: string;
  /** Date à partir de laquelle une levée précoce de l'IE est demandable (ISO yyyy-MM-dd). */
  datePossibleLevePrecoce: string;
  /** Date limite de dépôt du recours en annulation devant le CCE (ISO yyyy-MM-dd). */
  dateLimiteRecoursAnnulation: string;
  /** Jours calendaires restants pour le recours — peut être négatif si dépassé. */
  joursRestantsRecours: number;
  statutRecours: Annexe13quinquiesStatutRecours;
  /** Conditions pour solliciter la levée / le retrait de l'interdiction d'entrée. */
  conditionsLevee: string[];
  baseJuridique?: string;
}

export interface MotifInterdictionEntreeOption {
  code: MotifInterdictionEntree;
  label: string;
}

export const ANNEXE13QUINQUIES_MOTIFS: ReadonlyArray<MotifInterdictionEntreeOption> = [
  { code: 'SEJOUR_IRREGULIER', label: 'Séjour irrégulier (pas de délai de départ accordé)' },
  { code: 'MENACE_ORDRE_PUBLIC', label: "Menace pour l'ordre public" },
  { code: 'RAISONS_SECURITE_NATIONALE', label: 'Raisons de sécurité nationale' },
  { code: 'ATTEINTE_INTERET_UE', label: "Atteinte grave à l'intérêt de l'UE" },
  { code: 'DECISION_JUDICIAIRE', label: 'Décision judiciaire (juge)' },
];
