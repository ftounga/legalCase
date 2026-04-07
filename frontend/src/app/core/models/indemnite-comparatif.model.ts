export interface IndemniteComparatifRequest {
  country: string;
  ancienneteAnnees: number;
  age: number;
  salaireMensuel: number;
}

export interface IndemniteComparatifResponse {
  caseFileId: string;
  country: string;
  ancienneteAnnees: number;
  age: number;
  salaireMensuel: number;
  baremePlancherMois: number;
  baremePlafondMois: number;
  fourchetteBasseMois: number;
  fourchetteMedMois: number;
  fourhetteHauteMois: number;
  fourchetteBasseMontant: number;
  fourchetteMedMontant: number;
  fourhetteHauteMontant: number;
  baremeSource: string;
  commentaire: string;
}
