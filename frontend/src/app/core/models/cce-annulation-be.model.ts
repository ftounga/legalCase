/**
 * SF-215-13 / SF-215-14 — modèles miroirs du contrat API (backend) pour l'outil
 * décisionnel « Recours CCE annulation 30j (BE) » (F-IM-31-cce-annulation-30j-be).
 *
 * ⚠️ « CCE » = Conseil du Contentieux des Étrangers (juridiction administrative
 * belge en droit des étrangers) — AUCUN rapport avec le crédit/la banque.
 *
 * BELGIQUE uniquement — calculateur de délai du recours en ANNULATION devant le
 * CCE : 30 jours CALENDAIRES à compter de la notification de la décision attaquée
 * (Loi du 15/12/1980, art. 39/2 §2 et art. 39/57 §1er — délai de droit commun).
 */

/** Type de décision attaquée — whitelist stricte (miroir backend). */
export type CceAnnulationTypeDecision =
  | 'REFUS_TITRE'
  | 'REFUS_REGROUPEMENT'
  | 'REFUS_9BIS'
  | 'REFUS_9TER'
  | 'OQT_ANNEXE13'
  | 'DECISION_CGRA'
  | 'AUTRE';

/**
 * Statut du recours :
 *  - DISPONIBLE     : délai ouvert, marge confortable (vert)
 *  - URGENT         : délai ouvert mais proche de l'échéance (orange)
 *  - EXPIRE         : délai dépassé (rouge)
 *  - RECOURS_FORME  : un recours a déjà été formé (bleu info)
 */
export type CceAnnulationStatut =
  | 'DISPONIBLE'
  | 'URGENT'
  | 'EXPIRE'
  | 'RECOURS_FORME';

export interface CceAnnulationBeRequest {
  /** Date de notification de la décision attaquée (ISO yyyy-MM-dd). */
  dateNotificationDecision: string;
  typeDecision: CceAnnulationTypeDecision;
  /** Un recours a-t-il déjà été formé ? */
  recoursForme: boolean;
  /** Date de dépôt du recours (ISO yyyy-MM-dd) — requis si recoursForme=true. */
  dateRecours?: string | null;
}

export interface CceAnnulationBeResponse {
  caseFileId: string;
  dateNotificationDecision: string;
  typeDecision: string;
  recoursForme: boolean;
  dateRecours?: string | null;
  /** Date limite de dépôt du recours en annulation (ISO yyyy-MM-dd). */
  dateLimiteRecours: string;
  /** Jours calendaires restants — peut être négatif si le délai est dépassé. */
  joursRestants: number;
  statut: CceAnnulationStatut;
  /** Présent si statut URGENT/EXPIRE — référence l'outil F-IM-32. */
  recommandation?: string | null;
  /** Délai de dépôt du mémoire de synthèse / observations. */
  delaisMemoire: string;
  baseJuridique?: string;
}

export interface CceAnnulationTypeDecisionOption {
  code: CceAnnulationTypeDecision;
  label: string;
}

export const CCE_ANNULATION_TYPES_DECISION: ReadonlyArray<CceAnnulationTypeDecisionOption> = [
  { code: 'REFUS_TITRE', label: 'Refus de titre de séjour' },
  { code: 'REFUS_REGROUPEMENT', label: 'Refus de regroupement familial' },
  { code: 'REFUS_9BIS', label: 'Refus de séjour 9bis (régularisation)' },
  { code: 'REFUS_9TER', label: 'Refus de séjour 9ter (médical)' },
  { code: 'OQT_ANNEXE13', label: 'Ordre de quitter le territoire (annexe 13)' },
  { code: 'DECISION_CGRA', label: 'Décision du CGRA (asile)' },
  { code: 'AUTRE', label: 'Autre décision attaquable' },
];

/** Délai de droit commun du recours en annulation devant le CCE (jours calendaires). */
export const CCE_ANNULATION_DELAI_JOURS = 30;
