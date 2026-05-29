/**
 * SF-214-26 : modèles miroirs du contrat API (backend SF-214-25) pour l'outil
 * décisionnel « ANEF procédure / pannes » (F-IM-37). FR uniquement.
 *
 * Guide pas-à-pas de la dématérialisation ANEF (Administration Numérique des
 * Étrangers en France) : détecte une panne ANEF signalée, propose les étapes
 * alternatives de recours (dépôt préfecture, référé), et donne le délai de
 * recours pour faute de l'administration.
 */

export type StatutAnefProcedure =
  | 'NORMAL'
  | 'URGENT'
  | 'PANNE_EN_COURS'
  | 'RECOURS_POSSIBLE';

export interface AnefProcedureRequest {
  typeTitreConcerne: string;
  dateExpirationTitre: string; // ISO yyyy-MM-dd
  panneeANEFSignalee: boolean;
  dateTentativeDepot?: string | null; // ISO yyyy-MM-dd (optionnel)
  demandeAdresseePrefecture: boolean;
}

export interface AnefProcedureResponse {
  caseFileId: string;
  country: string;
  typeTitreConcerne: string;
  dateExpirationTitre: string;
  panneeANEFSignalee: boolean;
  dateTentativeDepot?: string | null;
  demandeAdresseePrefecture: boolean;
  statut: StatutAnefProcedure;
  etapesAlternatives: string[];
  etapesStandard: string[];
  delaiRecoursForFaute: string;
}
