/**
 * SF-221-01 — modèles miroirs du contrat API (backend) pour l'outil décisionnel
 * « Prorogation de la carte A (séjour temporaire BE) » (F-IM-53-carte-a-prorogation-be).
 *
 * BELGIQUE uniquement — calculateur du délai de dépôt (30-45 j avant expiration)
 * et des conditions de prorogation de la carte A (séjour temporaire / limité),
 * instruite par la commune (Loi 15/12/1980 art. 13 + AR 08/10/1981 art. 33 —
 * à vérifier par avocat).
 *
 * DISTINCT de F-IM-48 (passage carte A → séjour illimité après 5 ans), de la
 * délivrance initiale du titre et du renouvellement single permit (F-IM-25, travail).
 */

/**
 * Verdict de l'analyse :
 *  - PROROGEABLE            : conditions réunies + dans la fenêtre (vert)
 *  - A_DEPOSER_URGENT       : limite recommandée dépassée, carte non expirée (orange)
 *  - CONDITIONS_NON_REUNIES : motif/condition disparu (rouge)
 *  - EXPIREE                : carte expirée, pas de demande (rouge)
 *  - DEMANDE_DEPOSEE        : demande déjà déposée (bleu info)
 */
export type CarteAProrogationVerdict =
  | 'PROROGEABLE'
  | 'A_DEPOSER_URGENT'
  | 'CONDITIONS_NON_REUNIES'
  | 'EXPIREE'
  | 'DEMANDE_DEPOSEE';

export interface CarteAProrogationBeRequest {
  /** Date d'expiration de la carte A (ISO yyyy-MM-dd). */
  dateExpirationCarteA: string;
  motifSejourPersiste: boolean;
  conditionsInitialesToujoursReunies: boolean;
  demandeDeposee: boolean;
  /** Date de dépôt de la demande (ISO yyyy-MM-dd) — requise si demandeDeposee=true. */
  dateDemande?: string | null;
}

export interface CarteAProrogationBeResponse {
  caseFileId: string;
  dateExpirationCarteA: string;
  motifSejourPersiste: boolean;
  conditionsInitialesToujoursReunies: boolean;
  demandeDeposee: boolean;
  dateDemande?: string | null;
  verdict: CarteAProrogationVerdict;
  /** Jours calendaires avant l'expiration — peut être négatif si la carte est expirée. */
  joursAvantExpiration: number;
  /** Date d'ouverture de la fenêtre de dépôt (expiration − 45 j, ISO yyyy-MM-dd). */
  dateOuvertureFenetre: string;
  /** Date limite recommandée de dépôt (expiration − 30 j, ISO yyyy-MM-dd). */
  dateLimiteRecommandee: string;
  basesJuridiques: string[];
  messages: string[];
}

/** Borne haute de la fenêtre de dépôt — 45 jours avant l'expiration. */
export const CARTE_A_PROROGATION_FENETRE_OUVERTURE_JOURS = 45;

/** Borne basse (limite recommandée) — 30 jours avant l'expiration. */
export const CARTE_A_PROROGATION_FENETRE_LIMITE_JOURS = 30;
