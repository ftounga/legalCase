export interface AnalysisJob {
  jobType: 'CHUNK_ANALYSIS' | 'DOCUMENT_ANALYSIS' | 'CASE_ANALYSIS' | 'QUESTION_GENERATION' | 'ENRICHED_ANALYSIS';
  status: 'PENDING' | 'PROCESSING' | 'DONE' | 'FAILED';
  totalItems: number;
  processedItems: number;
  progressPercentage: number;
}
