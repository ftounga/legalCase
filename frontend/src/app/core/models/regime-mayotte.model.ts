/**
 * SF-220-02 : modèles miroirs du contrat API (backend SF-220-02) pour l'outil
 * décisionnel "Portée territoriale du titre à Mayotte"
 * (F-IM-48-regime-mayotte-fr). FR uniquement.
 *
 * Analyse la portée territoriale d'un titre délivré à Mayotte (Ord. 2014-464,
 * CESEDA L.832-1 et s.) : un titre mahorais ne vaut PAS autorisation de circuler
 * ou de séjourner en métropole sans démarche spécifique. Objet = dérogation
 * territoriale, pas le choix du titre (voir F-IM-05 pour cela).
 */

export type TypeTitreMayotte =
  | 'VPF'
  | 'SALARIE'
  | 'ETUDIANT'
  | 'RESIDENT'
  | 'AUTRE';

export type PorteeTerritorialeMayotte =
  | 'MAYOTTE_UNIQUEMENT'
  | 'DROIT_COMMUN';

export type SousStatutDeplacementMayotte =
  | 'BLOCAGE_DEPLACEMENT'
  | 'DEPLACEMENT_LIBRE';

export interface RegimeMayotteRequest {
  titreDelivreAMayotte: boolean;
  typeTitre: TypeTitreMayotte;
  projetDeplacementMetropole: boolean;
  dateDelivrance: string | null;
}

export interface RegimeMayotteResponse {
  caseFileId: string;
  titreDelivreAMayotte: boolean;
  typeTitre: TypeTitreMayotte;
  projetDeplacementMetropole: boolean;
  dateDelivrance: string | null;
  country: string;
  porteeTerritoriale: PorteeTerritorialeMayotte;
  sousStatutDeplacement: SousStatutDeplacementMayotte;
  obligationsSpecifiques: string[];
  demarchesDeplacementMetropole: string[];
  basesJuridiques: string[];
  messages: string[];
}
