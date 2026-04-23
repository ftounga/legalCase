export type ExtractionMethod = 'CLASSIC' | 'OCR' | 'NONE';
export type ExtractionStatus = 'PENDING' | 'PROCESSING' | 'DONE' | 'FAILED';

/** SF-127-01 : payload renvoyé par GET /documents/{id}/preview */
export interface DocumentPreview {
  fileName: string;
  mimeType: string;
  fileSize: number;
  pageCount: number | null;
  uploadedAt: string;
  extractionStatus: ExtractionStatus;
  extractionMethod: ExtractionMethod;
  extractedText: string | null;
  charCount: number;
  textTruncated: boolean;
  ocrPagesUsed: number;
  failureReason: string | null;
  /** SF-149-01 : timestamp ISO de la dernière édition manuelle, null si jamais édité */
  textEditedAt: string | null;
}
