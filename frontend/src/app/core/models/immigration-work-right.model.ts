export interface WorkRightRequest {
  titreType: string;
  country: string;
}

export interface WorkRightResponse {
  caseFileId: string;
  titreType: string;
  titreLabel: string;
  country: string;
  droitTravail: string;
  conditions: string;
  obligationsEmployeur: string[];
  baseJuridique: string;
}
