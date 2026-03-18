export interface AnalysisJob {
  jobType: 'CHUNK_ANALYSIS' | 'DOCUMENT_ANALYSIS' | 'CASE_ANALYSIS' | 'QUESTION_GENERATION';
  status: 'PENDING' | 'PROCESSING' | 'DONE' | 'FAILED';
  totalItems: number;
  processedItems: number;
  progressPercentage: number;
}
