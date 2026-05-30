/**
 * SF-218-02 : modèles miroirs du contrat API (backend SF-218-01) pour l'outil
 * décisionnel « Appel CPH cour d'appel » (F-DT-86-appel-cph-cour-appel).
 * FRANCE uniquement — appel d'un jugement du Conseil de prud'hommes (CPH)
 * devant la chambre sociale de la Cour d'appel (art. 538 CPC ; R. 1461-1 CPC).
 *
 * Calculateur de délai d'appel d'un mois : date limite d'appel + jours restants
 * + verdict de recevabilité (4 états) + checklist des formalités de l'appel
 * social (déclaration RPVA, chefs critiqués art. 901 CPC, représentation
 * obligatoire R. 1461-2 CPC…) + renvoi vers le pourvoi en cassation (F-DT-87)
 * lorsque la voie d'appel est fermée (jugement en dernier ressort, R. 1462-1 CPC).
 */

export type AppelCphPartieAppelante = 'SALARIE' | 'EMPLOYEUR';

export type AppelCphModeNotification = 'SIGNIFICATION' | 'LRAR';

export type AppelCphRepresentation = 'AVOCAT' | 'DEFENSEUR_SYNDICAL' | 'AUCUNE';

/**
 * Verdict de recevabilité de l'appel (aligné EXACTEMENT sur l'enum backend
 * {@code AppelCphVerdict}) :
 *  - DELAI_OUVERT : délai d'un mois en cours, plus de 7 jours restants.
 *  - DELAI_URGENT : 1 à 7 jours restants — déclaration d'appel à formaliser sans délai.
 *  - DELAI_EXPIRE : délai d'un mois expiré — appel irrecevable.
 *  - VOIE_FERMEE : jugement en dernier ressort — pas d'appel, seul un pourvoi
 *    en cassation est ouvert (renvoi F-DT-87).
 */
export type AppelCphVerdict =
  | 'DELAI_OUVERT'
  | 'DELAI_URGENT'
  | 'DELAI_EXPIRE'
  | 'VOIE_FERMEE';

/** Item de la checklist des formalités de l'appel — miroir de AppelCphChecklistItem. */
export interface AppelCphChecklistItem {
  libelle: string;
  obligatoire: boolean;
  bloquant: boolean;
  baseJuridique: string;
}

export interface AppelCphRequest {
  dateNotificationJugement: string; // YYYY-MM-DD — requis
  partieAppelante: AppelCphPartieAppelante; // SALARIE | EMPLOYEUR
  modeNotification: AppelCphModeNotification; // SIGNIFICATION | LRAR
  representationConstituee: AppelCphRepresentation; // AVOCAT | DEFENSEUR_SYNDICAL | AUCUNE
  jugementEnDernierRessort: boolean; // taux de compétence atteint → voie d'appel fermée
}

export interface AppelCphResponse {
  caseFileId: string;
  dateNotificationJugement: string;
  partieAppelante: AppelCphPartieAppelante;
  modeNotification: AppelCphModeNotification;
  representationConstituee: AppelCphRepresentation;
  jugementEnDernierRessort: boolean;
  dateLimiteAppel: string; // YYYY-MM-DD (notification + 1 mois)
  joursRestants: number; // jours calendaires restants (négatif si expiré)
  verdict: AppelCphVerdict;
  checklist: AppelCphChecklistItem[];
  renvoiPourvoiCassation: string | null; // renvoi F-DT-87 si voie fermée, null sinon
  country: string;
  baseJuridique: string;
}
