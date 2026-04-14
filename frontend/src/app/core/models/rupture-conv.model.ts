export interface RuptureConvCritereData {
  code: string;
  label: string;
  reponse: string;
  pointsRisque: number;
  bloquant: boolean;
  commentaire: string;
}

export interface RuptureConvRequest {
  country: string;
  reponses: Record<string, string>;
}

export interface RuptureConvResponse {
  caseFileId: string;
  country: string;
  scoreRisque: number;
  verdict: string;
  criteres: RuptureConvCritereData[];
}
