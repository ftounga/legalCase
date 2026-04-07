export interface PartageImmobilierRequest {
  country: string;
  valeurVenale: number;
  capitalRestantDu: number;
  quotePartAttributaire: number;
  isDivorce: boolean;
}

export interface PartageImmobilierResponse {
  caseFileId: string;
  country: string;
  valeurVenale: number;
  capitalRestantDu: number;
  valeurNette: number;
  quotePartAttributaire: number;
  quotePartCedant: number;
  partAttributaire: number;
  partCedant: number;
  soulte: number;
  droitPartage: number;
  tauxDroitPartage: number;
  fraisNotaireEstimes: number;
  coutTotal: number;
  baseJuridique: string;
  commentaire: string;
}
