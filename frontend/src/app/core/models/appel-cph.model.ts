/**
 * SF-218-02 : modèles miroirs du contrat API (backend SF-218-01) pour l'outil
 * décisionnel « Appel CPH devant la Cour d'appel » (F-DT-86-appel-cph-cour-appel).
 * FRANCE uniquement — appel d'un jugement du conseil de prud'hommes devant la
 * chambre sociale de la cour d'appel (délai d'appel d'1 mois, art. 538 CPC ;
 * R. 1461-1 et s. du Code du travail).
 *
 * Calculateur de délai d'appel + verdict de recevabilité + checklist des
 * formalités d'appel social.
 */

export type PartieAppelante = 'SALARIE' | 'EMPLOYEUR';

export type ModeNotification = 'SIGNIFICATION' | 'LRAR';

export type RepresentationConstituee = 'AVOCAT' | 'DEFENSEUR_SYNDICAL' | 'AUCUNE';

/**
 * Verdict de recevabilité de l'appel :
 * - `DELAI_OUVERT`  : appel encore largement recevable (vert).
 * - `DELAI_URGENT`  : délai court (≤ 7 jours restants) — à former sans délai (or).
 * - `DELAI_EXPIRE`  : délai d'appel dépassé — appel irrecevable (rouge).
 * - `VOIE_FERMEE`   : jugement rendu en premier et dernier ressort — pas d'appel,
 *   seul le pourvoi en cassation est ouvert (navy, lien vers F-DT-87).
 */
export type VerdictAppelCph =
  | 'DELAI_OUVERT'
  | 'DELAI_URGENT'
  | 'DELAI_EXPIRE'
  | 'VOIE_FERMEE';

export interface AppelCphChecklistItem {
  libelle: string;
  obligatoire: boolean;
  baseJuridique: string;
}

export interface AppelCphRequest {
  dateNotificationJugement: string; // YYYY-MM-DD — requis
  partieAppelante: PartieAppelante;
  modeNotification: ModeNotification;
  representationConstituee: RepresentationConstituee;
  jugementEnDernierRessort: boolean;
}

export interface AppelCphResponse {
  caseFileId: string;
  dateNotificationJugement: string;
  partieAppelante: PartieAppelante;
  modeNotification: ModeNotification;
  representationConstituee: RepresentationConstituee;
  jugementEnDernierRessort: boolean;
  dateLimiteAppel: string; // YYYY-MM-DD
  joursRestants: number;
  verdict: VerdictAppelCph;
  checklist: AppelCphChecklistItem[];
  baseJuridique: string;
}
