/**
 * SF-214-40 : modèles miroirs du contrat API (backend SF-214-39) pour l'outil
 * décisionnel "Séjour UE/EEE/Suisse" (F-IM-44-ue-eee-suisse-sejour-fr).
 * FRANCE uniquement — droit au séjour des citoyens de l'Union européenne, de
 * l'Espace économique européen et de la Suisse (et de leurs membres de famille),
 * directive 2004/38/CE transposée aux art. L. 233-1 et s. du CESEDA.
 *
 * Analyseur de droits : droit au séjour automatique de 3 mois, droit au séjour
 * permanent au-delà de 5 ans, titre obtenu (attestation d'enregistrement ou
 * carte de séjour membre de famille), conditions respectées, situation du membre
 * de famille non-UE, base juridique.
 */

export type ActiviteProfessionnelle =
  | 'SALARIE'
  | 'INDEPENDANT'
  | 'ETUDIANT'
  | 'RETRAITE'
  | 'SANS_ACTIVITE_RESSOURCES_SUFFISANTES';

export type TitreSejourUe =
  | 'ATTESTATION_ENREGISTREMENT'
  | 'CARTE_SEJOUR_MEMBRE_FAMILLE';

export interface UeEeeSuisseSejourRequest {
  nationalite: string; // nationalité du ressortissant — requis
  estCitoyenUE: boolean; // citoyen UE/EEE/Suisse ?
  membreFamilleNonUE: boolean; // membre de famille de nationalité non-UE ?
  dureeSejourMois: number; // durée du séjour en France (mois)
  activiteProfessionnelle: ActiviteProfessionnelle;
}

export interface UeEeeSuisseSejourResponse {
  caseFileId: string;
  nationalite: string;
  estCitoyenUE: boolean;
  membreFamilleNonUE: boolean;
  dureeSejourMois: number;
  activiteProfessionnelle: ActiviteProfessionnelle;
  country: string;
  droitSejourAutomatique3Mois: boolean;
  droitSejourPlus5Ans: boolean;
  titreObtenu: TitreSejourUe;
  conditionsRespectees: string[];
  situationMembreNonUE?: string | null;
  baseJuridique: string;
}
