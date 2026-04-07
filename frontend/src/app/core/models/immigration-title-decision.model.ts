export interface TitleRecommendation {
  code: string;
  label: string;
  country: string;
  motif: string;
  conditions: string;
  pieces: string[];
  delaiMoyenJours: number;
}

export interface TitleDecisionRequest {
  country: string;
  nationaliteUe: boolean;
  motif: string;
  duree: string;
  situationFamiliale: string | null;
}

export interface TitleDecisionResponse {
  caseFileId: string;
  country: string;
  nationaliteUe: boolean;
  motif: string;
  duree: string;
  situationFamiliale: string | null;
  recommendations: TitleRecommendation[];
}
