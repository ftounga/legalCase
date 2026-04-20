export type IndemniteDisplayMode = 'MACRON' | 'CCT_109' | 'INDEMNITE_SPECIFIQUE' | 'NEGOCIATION_LIBRE'; // NEGOCIATION_LIBRE conservé pour rétrocompat transitoire (ancien dossiers persistés) — plus produit par le backend depuis SF-132-03

export interface IndemniteComparatifRequest {
  country: string;
  typeRupture: string;
  ancienneteAnnees: number;
  ancienneteMois: number;
  age: number;
  salaireMensuel: number;
}

export interface IndemniteComparatifResponse {
  caseFileId: string;
  country: string;
  typeRupture: string | null;
  ancienneteAnnees: number;
  ancienneteMois: number;
  age: number;
  salaireMensuel: number;
  displayMode: IndemniteDisplayMode;
  baremePlancherMois: number;
  baremePlafondMois: number;
  fourchetteBasseMois: number;
  fourchetteMedMois: number;
  fourhetteHauteMois: number;
  fourchetteBasseMontant: number;
  fourchetteMedMontant: number;
  fourhetteHauteMontant: number;
  indemniteLegaleMontant: number | null;
  baremeSource: string;
  commentaire: string;
  contextualMessages: string[];
}
