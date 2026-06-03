/**
 * SF-221-04 — modèles miroirs du contrat API (backend) pour l'outil décisionnel
 * « Détention en centre fermé + requête de mise en liberté (BE) »
 * (F-IM-56-detention-centre-ferme-be).
 *
 * BELGIQUE uniquement — calcule la durée de la détention administrative en centre fermé
 * (art. 7 al. 3 / 27 / 29 / 74/5 Loi 15/12/1980 ; AR 02/08/2002) et cadre la requête de
 * mise en liberté devant la CHAMBRE DU CONSEIL (art. 71 et s. ; fenêtre indicative 5 j) —
 * à vérifier par avocat.
 *
 * Une situation fusionnée : la détention ET son recours. La chambre du conseil est une
 * juridiction JUDICIAIRE, DISTINCTE du CCE (F-IM-31 annulation 30j, F-IM-32 extrême
 * urgence 5j, F-IM-57 suspension).
 */

/** Base légale du maintien — whitelist de 5 valeurs. */
export type DetentionBaseLegale = 'ART_7' | 'ART_27' | 'ART_29' | 'ART_74_5' | 'AUTRE';

/**
 * Verdict de l'analyse :
 *  - DETENTION_EN_COURS        : maintien constaté, fenêtre de requête non connue (bleu info / défaut)
 *  - REQUETE_OUVERTE           : fenêtre 5 j encore ouverte, requête non déposée (vert)
 *  - REQUETE_TARDIVE           : fenêtre 5 j dépassée, requête non déposée (orange)
 *  - REQUETE_DEPOSEE           : requête déjà introduite devant la chambre du conseil (bleu info)
 *  - PROLONGATION_A_CONTESTER  : prolongation notifiée, nouvelle fenêtre ouverte (vert)
 */
export type DetentionCentreFermeBeVerdict =
  | 'DETENTION_EN_COURS'
  | 'REQUETE_OUVERTE'
  | 'REQUETE_TARDIVE'
  | 'REQUETE_DEPOSEE'
  | 'PROLONGATION_A_CONTESTER';

export interface DetentionCentreFermeBeRequest {
  /** Date de début de la détention (ISO yyyy-MM-dd, non future). */
  dateDebutDetention: string;
  baseLegaleDetention: DetentionBaseLegale;
  prolongationNotifiee: boolean;
  /** Requise si prolongationNotifiee=true (ISO yyyy-MM-dd). */
  dateProlongation: string | null;
  requeteMiseEnLiberteDeposee: boolean;
  /** Requise si requeteMiseEnLiberteDeposee=true ; point de départ de la fenêtre 5 j (ISO yyyy-MM-dd). */
  dateNotificationDecisionDetention: string | null;
}

export interface DetentionCentreFermeBeResponse {
  caseFileId: string;
  dateDebutDetention: string;
  baseLegaleDetention: DetentionBaseLegale;
  prolongationNotifiee: boolean;
  dateProlongation: string | null;
  requeteMiseEnLiberteDeposee: boolean;
  dateNotificationDecisionDetention: string | null;
  verdict: DetentionCentreFermeBeVerdict;
  /** Jours écoulés depuis dateDebutDetention. */
  dureeDetentionJours: number;
  /** Date limite indicative de la requête (point de départ + 5 j). Null si non calculable. */
  dateLimiteRequete: string | null;
  /** Jours restants dans la fenêtre de 5 j (0 si dépassée). Null si non calculable. */
  joursRestantsRequete: number | null;
  basesJuridiques: string[];
  messages: string[];
}

/** Fenêtre indicative de requête devant la chambre du conseil — 5 jours calendaires. */
export const DETENTION_FENETRE_REQUETE_JOURS = 5;
