/**
 * SF-214-24 : modèles miroirs du contrat API (backend SF-214-23) pour l'outil
 * décisionnel « Carte de résident — article L. 426-1 du CESEDA » (F-IM-36).
 * FR uniquement (régime distinct en BE).
 *
 * Évalue l'éligibilité à la carte de résident de dix ans de l'article L. 426-1
 * du CESEDA (durée de séjour régulier, niveau d'intégration républicaine,
 * ressources stables et suffisantes, absence de condamnations pénales graves),
 * et liste les critères non remplis (checklist) ainsi que les atouts du dossier.
 */

export type CarteResidentVerdict =
  | 'ELIGIBLE'
  | 'ELIGIBLE_SOUS_RESERVE'
  | 'NON_ELIGIBLE_DELAI'
  | 'NON_ELIGIBLE_INTEGRATION'
  | 'NON_ELIGIBLE_RESSOURCES'
  | 'INADMISSIBLE';

export type CarteResidentNiveauIntegration = 'FORT' | 'MOYEN' | 'FAIBLE';

export interface CarteResidentRequest {
  dureeSejourRegulierAnnees: number;
  typesTitresAnterieurs?: string | null; // optionnel
  niveauIntegration: CarteResidentNiveauIntegration;
  ressourcesMensuellesNettes: number;
  condamnationsPenalesGraves: boolean;
}

export interface CarteResidentResponse {
  caseFileId: string;
  country: string;
  verdict: CarteResidentVerdict;
  chipsCriteresNonRemplis: string[];
  atouts: string[];
  baseJuridique: string;
}
