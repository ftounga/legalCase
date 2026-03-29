export interface SynthesisSearchResult {
  caseFileId: string;
  caseFileTitle: string;
  legalDomain: string;
  analysisType: string;
  matchCount: number;
  excerpts: string[];
}

export interface SynthesisSearchResponse {
  query: string;
  totalResults: number;
  results: SynthesisSearchResult[];
}
