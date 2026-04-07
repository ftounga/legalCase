export interface LicenciementCritereData {
  code: string;
  label: string;
  reponse: string;
  pointsRisque: number;
  bloquant: boolean;
  commentaire: string;
}

export interface LicenciementRequest {
  country: string;
  reponses: Record<string, string>;
}

export interface LicenciementResponse {
  caseFileId: string;
  country: string;
  scoreRisque: number;
  verdict: string;
  criteres: LicenciementCritereData[];
}
