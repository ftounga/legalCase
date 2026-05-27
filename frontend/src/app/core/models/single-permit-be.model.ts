/**
 * SF-215-01 / SF-215-02 — modèles miroirs du contrat API (backend) pour l'outil
 * décisionnel « Permis unique BE » (F-IM-25-single-permit-be).
 *
 * BELGIQUE uniquement — combine travail + séjour (single permit) sur la base de :
 *  - Loi 15/12/1980 art. 61/25-2 à 61/25-7
 *  - Accord de coopération du 02/02/2018 (Wallonie/Flandre/Bruxelles)
 *  - Décret CRWAPE 16/05/2019 (Wallonie — FOREM)
 *  - Arrêté du Gouvernement wallon du 02/05/2019
 *  - Arrêté du Gouvernement flamand du 07/12/2018 (VDAB)
 *  - Ordonnance Bruxelloise du 09/07/2015 (ACTIRIS)
 */

export type SinglePermitRegion = 'WALLONIE' | 'FLANDRE' | 'BRUXELLES';

export type SinglePermitTypeActivite =
  | 'SALARIE'
  | 'STAGIAIRE'
  | 'DETACHE'
  | 'CHERCHEUR'
  | 'ETUDIANT';

export type SinglePermitMotif = 'NOUVEAU' | 'RENOUVELLEMENT';

export type SinglePermitStatutRenouvellement =
  | 'DEPOSE_EN_TEMPS'
  | 'DANS_DELAI'
  | 'URGENT'
  | 'EXPIRE';

export interface SinglePermitBeRequest {
  dateDebutPermit: string;            // YYYY-MM-DD
  dateFinPermit: string;              // YYYY-MM-DD
  regionInstruction: SinglePermitRegion;
  typeActivite: SinglePermitTypeActivite;
  motifDemande: SinglePermitMotif;
}

export interface SinglePermitBeResponse {
  caseFileId: string;
  dateDebutPermit: string;
  dateFinPermit: string;
  regionInstruction: string;
  typeActivite: string;
  motifDemande: string;
  dateLimiteDemande: string;          // YYYY-MM-DD (dateFinPermit - 60 jours)
  joursAvantExpiration: number;
  statutRenouvellement: SinglePermitStatutRenouvellement;
  regionCompetente: string;
  etapesProchaines: string[];
  baseJuridique: string;
}
