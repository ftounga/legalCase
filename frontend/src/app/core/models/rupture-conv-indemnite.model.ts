export interface RuptureConvIndemniteRequest {
  ancienneteAnnees: number;
  salaireMensuel: number;
}

export interface RuptureConvIndemniteResponse {
  caseFileId: string;
  ancienneteAnnees: number;
  salaireMensuel: number;
  indemniteLegaleMinimum: number;
  formule: string;
  baseJuridique: string;
  messages: string[];
}
