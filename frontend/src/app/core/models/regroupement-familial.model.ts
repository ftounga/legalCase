/**
 * SF-214-04 : modèles miroirs du contrat API (backend SF-214-03) pour l'outil
 * décisionnel "Regroupement familial L.434-1+ CESEDA" (F-IM-26).
 * FR uniquement (CESEDA L.434-1 à L.434-10, R.434-1+).
 */

export type TypeRegroupement = 'CONJOINT' | 'ENFANT_MINEUR' | 'AUTRE';

export type VerdictRegroupementFamilial =
  | 'ELIGIBLE'
  | 'ELIGIBLE_SOUS_RESERVE'
  | 'NON_ELIGIBLE_DELAI'
  | 'NON_ELIGIBLE_RESSOURCES'
  | 'NON_ELIGIBLE_LOGEMENT';

export interface RegroupementFamilialRequest {
  dureeSejourRegulierMois: number;
  ressourcesMensuellesNettes: number;
  tailleLogementM2: number;
  nombrePersonnesFoyer: number;
  typeRegroupement: TypeRegroupement;
  membresFamilleARegrouper: number; // 1-6
}

export interface RegroupementFamilialResponse {
  caseFileId: string;
  dureeSejourRegulierMois: number;
  ressourcesMensuellesNettes: number;
  tailleLogementM2: number;
  nombrePersonnesFoyer: number;
  typeRegroupement: TypeRegroupement;
  membresFamilleARegrouper: number;
  country: string;
  verdict: VerdictRegroupementFamilial;
  ressourcesRequises: number;
  surfaceRequise: number;
  chipsCriteresNonRemplis: string[];
  baseJuridique: string;
}
