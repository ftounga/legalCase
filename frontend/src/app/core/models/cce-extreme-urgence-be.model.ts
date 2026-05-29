/**
 * SF-215-15 / SF-215-16 — modèles miroirs du contrat API (backend) pour l'outil
 * décisionnel « Recours CCE extrême urgence 5j (BE) » (F-IM-32-cce-extreme-urgence-5j-be).
 *
 * ⚠️ « CCE » = Conseil du Contentieux des Étrangers (juridiction administrative
 * belge en droit des étrangers) — AUCUN rapport avec le crédit/la banque.
 *
 * BELGIQUE uniquement — calculateur du délai du recours en EXTRÊME URGENCE devant
 * le CCE : 5 jours OUVRABLES à compter de la notification de l'acte exécutoire
 * (Loi du 15/12/1980, art. 39/82 §4 al. 2 — procédure d'extrême urgence). Cas
 * d'urgence absolue : OQT exécuté, transfert Dublin, refus d'accès au territoire,
 * expulsion imminente.
 */

/** Type d'acte exécutoire attaqué — whitelist stricte (miroir backend). */
export type CceExtremeUrgenceTypeActe =
  | 'OQT_EXECUTE'
  | 'TRANSFERT_DUBLIN'
  | 'REFUS_ACCES_TERRITOIRE'
  | 'EXPULSION_IMMEDIATE'
  | 'AUTRE';

/**
 * Statut du recours en extrême urgence :
 *  - DISPONIBLE     : délai ouvert, marge restante (vert)
 *  - CRITIQUE       : délai ouvert mais ≤ 2 jours ouvrables restants — urgence
 *    absolue, action immédiate requise (ROUGE PROÉMINENT)
 *  - EXPIRE         : délai dépassé (ROUGE PROÉMINENT)
 *  - RECOURS_FORME  : un recours a déjà été formé (bleu info)
 */
export type CceExtremeUrgenceStatut =
  | 'DISPONIBLE'
  | 'CRITIQUE'
  | 'EXPIRE'
  | 'RECOURS_FORME';

export interface CceExtremeUrgenceBeRequest {
  /** Date de l'acte exécutoire attaqué (ISO yyyy-MM-dd). */
  dateActeExecutoire: string;
  typeActe: CceExtremeUrgenceTypeActe;
  /** Un recours a-t-il déjà été formé ? */
  recoursForme: boolean;
  /** Date de dépôt du recours (ISO yyyy-MM-dd) — requis si recoursForme=true. */
  dateRecours?: string | null;
}

export interface CceExtremeUrgenceBeResponse {
  caseFileId: string;
  dateActeExecutoire: string;
  typeActe: string;
  recoursForme: boolean;
  dateRecours?: string | null;
  /** Date limite de dépôt du recours en extrême urgence (ISO yyyy-MM-dd). */
  dateLimiteRecours: string;
  /** Jours OUVRABLES belges restants — peut être négatif si le délai est dépassé. */
  joursOuvrablesRestants: number;
  statut: CceExtremeUrgenceStatut;
  /** Date d'audience CCE estimée (ISO yyyy-MM-dd) — procédure accélérée. */
  audienceEstimee?: string | null;
  /** Action immédiate à mener (message court) — affichée en bandeau rouge si CRITIQUE/EXPIRE. */
  actionImmediate?: string | null;
  baseJuridique?: string;
}

export interface CceExtremeUrgenceTypeActeOption {
  code: CceExtremeUrgenceTypeActe;
  label: string;
}

export const CCE_EXTREME_URGENCE_TYPES_ACTE: ReadonlyArray<CceExtremeUrgenceTypeActeOption> = [
  { code: 'OQT_EXECUTE', label: "Ordre de quitter le territoire exécuté / exécutoire" },
  { code: 'TRANSFERT_DUBLIN', label: 'Transfert Dublin imminent' },
  { code: 'REFUS_ACCES_TERRITOIRE', label: "Refus d'accès au territoire (frontière)" },
  { code: 'EXPULSION_IMMEDIATE', label: 'Mesure d\'expulsion immédiate' },
  { code: 'AUTRE', label: 'Autre acte exécutoire imminent' },
];

/** Délai du recours en extrême urgence devant le CCE (jours OUVRABLES, art. 39/82). */
export const CCE_EXTREME_URGENCE_DELAI_JOURS_OUVRABLES = 5;
